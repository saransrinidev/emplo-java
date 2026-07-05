package com.emplo.repository;

import com.emplo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findAllBySenderIdOrReceiverIdOrderByCreatedAtDesc(UUID senderId, UUID receiverId);

    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
            "(m.senderId = :user2 AND m.receiverId = :user1) " +
            "ORDER BY m.createdAt ASC")
    List<Message> findConversationBetween(@Param("user1") UUID user1, @Param("user2") UUID user2);

    long countByReceiverIdAndIsReadFalse(UUID receiverId);
}
