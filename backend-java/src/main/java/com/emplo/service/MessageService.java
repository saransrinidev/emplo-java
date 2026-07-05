package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.Message;
import com.emplo.entity.Role;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.MessageRepository;
import com.emplo.repository.RoleRepository;
import com.emplo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public Message sendMessage(User user, UUID receiverId, String content) {
        if (user.getEmployeeId() == null) {
            throw new BadRequestException("No employee record linked");
        }
        if (user.getEmployeeId().equals(receiverId)) {
            throw new BadRequestException("Cannot message yourself");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BadRequestException("Message cannot be empty");
        }
        if (employeeRepository.findById(receiverId).isEmpty()) {
            throw new NotFoundException("Receiver not found");
        }
        if (!canMessage(user, user.getEmployeeId(), receiverId)) {
            throw new ForbiddenException("You cannot message this person");
        }

        Message msg = Message.builder()
                .senderId(user.getEmployeeId())
                .receiverId(receiverId)
                .content(content.trim())
                .isRead(false)
                .build();
        return messageRepository.save(msg);
    }

    public List<Map<String, Object>> getConversations(User user) {
        if (user.getEmployeeId() == null) return List.of();
        UUID empId = user.getEmployeeId();
        List<Message> messages = messageRepository.findAllBySenderIdOrReceiverIdOrderByCreatedAtDesc(empId, empId);

        Map<UUID, Map<String, Object>> conversations = new LinkedHashMap<>();
        Map<UUID, Integer> unreadCounts = new HashMap<>();

        for (Message msg : messages) {
            UUID otherId = msg.getSenderId().equals(empId) ? msg.getReceiverId() : msg.getSenderId();
            if (!conversations.containsKey(otherId)) {
                Employee other = employeeRepository.findById(otherId).orElse(null);
                Map<String, Object> conv = new LinkedHashMap<>();
                conv.put("employee_id", otherId);
                conv.put("employee_name", other != null ? other.getFullName() : "Unknown");
                conv.put("employee_photo", other != null ? other.getProfilePhoto() : null);
                conv.put("last_message", msg.getContent().length() > 80 ? msg.getContent().substring(0, 80) : msg.getContent());
                conv.put("last_message_time", msg.getCreatedAt());
                conv.put("is_sender", msg.getSenderId().equals(empId));
                conversations.put(otherId, conv);
                unreadCounts.put(otherId, 0);
            }
            if (msg.getReceiverId().equals(empId) && !msg.getIsRead()) {
                unreadCounts.merge(otherId, 1, Integer::sum);
            }
        }
        conversations.forEach((id, conv) -> conv.put("unread_count", unreadCounts.getOrDefault(id, 0)));
        return new ArrayList<>(conversations.values());
    }

    @Transactional
    public List<Message> getConversation(User user, UUID employeeId) {
        if (user.getEmployeeId() == null) return List.of();
        UUID empId = user.getEmployeeId();
        List<Message> messages = messageRepository.findConversationBetween(empId, employeeId);

        // Mark unread messages as read
        Instant now = Instant.now();
        messages.stream()
                .filter(m -> m.getReceiverId().equals(empId) && !m.getIsRead())
                .forEach(m -> {
                    m.setIsRead(true);
                    m.setReadAt(now);
                });
        messageRepository.saveAll(messages);
        return messages;
    }

    public List<Map<String, Object>> getContacts(User user) {
        if (user.getEmployeeId() == null) return List.of();
        Employee emp = employeeRepository.findById(user.getEmployeeId()).orElse(null);
        if (emp == null) return List.of();

        if (user.getRole().getName() == RoleName.hr_admin) {
            return employeeRepository.findAll().stream()
                    .filter(e -> !e.getId().equals(user.getEmployeeId()))
                    .map(e -> contactMap(e, "Employee"))
                    .toList();
        }

        List<Map<String, Object>> contacts = new ArrayList<>();
        if (emp.getManagerId() != null) {
            employeeRepository.findById(emp.getManagerId())
                    .ifPresent(mgr -> contacts.add(contactMap(mgr, "Manager")));
        }
        employeeRepository.findAllByManagerId(user.getEmployeeId())
                .forEach(r -> contacts.add(contactMap(r, "Report")));
        if (emp.getManagerId() != null) {
            employeeRepository.findAllByManagerId(emp.getManagerId()).stream()
                    .filter(p -> !p.getId().equals(user.getEmployeeId()))
                    .forEach(p -> contacts.add(contactMap(p, "Peer")));
        }

        roleRepository.findByName(RoleName.hr_admin).ifPresent(hrRole -> {
            List<User> hrUsers = userRepository.findAllByRoleId(hrRole.getId());
            for (User hu : hrUsers) {
                if (hu.getEmployeeId() != null && !hu.getEmployeeId().equals(user.getEmployeeId())) {
                    boolean alreadyAdded = contacts.stream()
                            .anyMatch(c -> c.get("id").equals(hu.getEmployeeId().toString()));
                    if (!alreadyAdded) {
                        employeeRepository.findById(hu.getEmployeeId())
                                .ifPresent(hrEmp -> contacts.add(contactMap(hrEmp, "HR")));
                    }
                }
            }
        });

        return contacts;
    }

    public long getUnreadCount(User user) {
        if (user.getEmployeeId() == null) return 0;
        return messageRepository.countByReceiverIdAndIsReadFalse(user.getEmployeeId());
    }

    private boolean canMessage(User user, UUID senderId, UUID receiverId) {
        if (user.getRole().getName() == RoleName.hr_admin) return true;
        Employee sender = employeeRepository.findById(senderId).orElse(null);
        Employee receiver = employeeRepository.findById(receiverId).orElse(null);
        if (sender == null || receiver == null) return false;
        if (receiverId.equals(sender.getManagerId())) return true;
        if (senderId.equals(receiver.getManagerId())) return true;
        if (sender.getManagerId() != null && sender.getManagerId().equals(receiver.getManagerId())) return true;
        User receiverUser = userRepository.findByEmployeeId(receiverId).orElse(null);
        return receiverUser != null && receiverUser.getRole().getName() == RoleName.hr_admin;
    }

    private Map<String, Object> contactMap(Employee e, String role) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId().toString());
        map.put("name", e.getFullName());
        map.put("role", role);
        map.put("photo", e.getProfilePhoto());
        return map;
    }
}
