import { useState, useRef, useEffect } from "react";
import { MessageCircle, X, Send, Bot, User as UserIcon, Loader2, Sparkles, Trash2 } from "lucide-react";
import { chatApi } from "../api/chat";
import { useAuth } from "../context/AuthContext";

interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: Date;
}

const SUGGESTED_PROMPTS = [
  "How do I apply for leave?",
  "Where can I see my salary?",
  "How do I upload documents?",
  "What's on my onboarding checklist?",
];

function formatTime(date: Date): string {
  return date.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" });
}

export default function ChatWidget() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const welcomeMessage: ChatMessage = {
    id: "welcome",
    role: "assistant",
    content: `Hi ${user?.name?.split(" ")[0] || "there"}! I'm the Emplo Assistant. Ask me anything about leave, attendance, salary, documents, or onboarding.`,
    timestamp: new Date(),
  };

  useEffect(() => {
    if (messages.length === 0) {
      setMessages([welcomeMessage]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (open && inputRef.current) inputRef.current.focus();
  }, [open]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  if (!user) return null;

  const sendMessage = async (text: string) => {
    const msg = text.trim();
    if (!msg || loading) return;

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      role: "user",
      content: msg,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setLoading(true);

    try {
      const res = await chatApi.send(msg);
      setMessages((prev) => [
        ...prev,
        { id: (Date.now() + 1).toString(), role: "assistant", content: res.response, timestamp: new Date() },
      ]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          id: (Date.now() + 1).toString(),
          role: "assistant",
          content: "Sorry, I couldn't connect to the AI service. Please ensure Ollama is running and try again.",
          timestamp: new Date(),
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage(input);
    }
  };

  const clearChat = () => {
    setMessages([{ ...welcomeMessage, timestamp: new Date() }]);
  };

  const showSuggestions = messages.length <= 1 && !loading;

  return (
    <>
      {/* Floating button */}
      {!open && (
        <button className="chat-fab" onClick={() => setOpen(true)} aria-label="Open chat assistant">
          <MessageCircle size={22} />
          <span className="chat-fab-badge"><Sparkles size={10} /></span>
        </button>
      )}

      {/* Chat panel */}
      {open && (
        <div className="chat-panel">
          {/* Header */}
          <div className="chat-header">
            <div className="chat-header-left">
              <div className="chat-avatar">
                <Bot size={18} />
                <span className="chat-status-dot" />
              </div>
              <div>
                <div className="chat-header-title">Emplo Assistant</div>
                <div className="chat-header-subtitle">
                  <span className="chat-online-indicator" /> Online · AI-powered
                </div>
              </div>
            </div>
            <div className="chat-header-actions">
              <button className="chat-icon-btn" onClick={clearChat} title="Clear chat">
                <Trash2 size={15} />
              </button>
              <button className="chat-icon-btn" onClick={() => setOpen(false)} title="Close">
                <X size={17} />
              </button>
            </div>
          </div>

          {/* Messages */}
          <div className="chat-messages">
            {messages.map((msg) => (
              <div key={msg.id} className={`chat-msg chat-msg-${msg.role}`}>
                {msg.role === "assistant" && (
                  <div className="chat-msg-icon">
                    <Bot size={14} />
                  </div>
                )}
                <div className="chat-msg-wrapper">
                  <div className="chat-msg-bubble">
                    <p>{msg.content}</p>
                  </div>
                  <span className="chat-msg-time">{formatTime(msg.timestamp)}</span>
                </div>
              </div>
            ))}

            {loading && (
              <div className="chat-msg chat-msg-assistant">
                <div className="chat-msg-icon"><Bot size={14} /></div>
                <div className="chat-msg-wrapper">
                  <div className="chat-msg-bubble chat-typing">
                    <span className="chat-dot" />
                    <span className="chat-dot" />
                    <span className="chat-dot" />
                  </div>
                </div>
              </div>
            )}

            {/* Suggested prompts */}
            {showSuggestions && (
              <div className="chat-suggestions">
                <span className="chat-suggestions-label">Suggested questions</span>
                {SUGGESTED_PROMPTS.map((prompt) => (
                  <button key={prompt} className="chat-suggestion-chip" onClick={() => sendMessage(prompt)}>
                    {prompt}
                  </button>
                ))}
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Input */}
          <div className="chat-input-area">
            <input
              ref={inputRef}
              className="chat-input"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Type your question..."
              disabled={loading}
            />
            <button
              className="chat-send-btn"
              onClick={() => sendMessage(input)}
              disabled={!input.trim() || loading}
            >
              {loading ? <Loader2 size={16} className="chat-spinner" /> : <Send size={16} />}
            </button>
          </div>
          <div className="chat-footer-note">Emplo Assistant can only help with HR & product topics</div>
        </div>
      )}
    </>
  );
}
