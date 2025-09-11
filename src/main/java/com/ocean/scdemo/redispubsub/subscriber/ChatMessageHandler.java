package com.ocean.scdemo.redispubsub.subscriber;

import com.ocean.scdemo.redispubsub.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 채팅 메시지 처리 핸들러
 * 
 * 기능:
 * - 실시간 채팅 메시지 처리
 * - 파일 첨부 메시지 처리
 * - 멘션 및 답장 메시지 처리
 * - 채팅방별 메시지 라우팅
 * - 읽음 확인 처리
 * - 메시지 필터링 및 검열
 */
@Slf4j
@Component
public class ChatMessageHandler {

    // 처리 통계
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong textMessages = new AtomicLong(0);
    private final AtomicLong fileMessages = new AtomicLong(0);
    private final AtomicLong mentionMessages = new AtomicLong(0);
    private final AtomicLong replyMessages = new AtomicLong(0);
    
    // 채팅방별 활성 사용자 추적
    private final Map<String, Set<String>> activeUsersInRoom = new ConcurrentHashMap<>();
    
    // 최근 처리된 메시지 (중복 방지용)
    private final Map<String, LocalDateTime> recentMessages = new ConcurrentHashMap<>();
    
    public boolean handleMessage(ChatMessage message) {
        try {
            totalProcessed.incrementAndGet();
            
            // 중복 메시지 체크
            if (isDuplicateMessage(message)) {
                log.debug("중복 메시지 무시: ID={}", message.getMessageId());
                return true;
            }
            
            // 메시지 필터링 (부적절한 내용 검사)
            if (!isMessageAllowed(message)) {
                log.warn("부적절한 메시지 차단: ID={}, 발신자={}", 
                        message.getMessageId(), message.getSenderId());
                return false;
            }
            
            // 메시지 타입별 처리
            boolean processed = switch (message.getContentType()) {
                case "TEXT" -> handleTextMessage(message);
                case "FILE" -> handleFileMessage(message);
                case "IMAGE" -> handleImageMessage(message);
                case "AUDIO" -> handleAudioMessage(message);
                case "VIDEO" -> handleVideoMessage(message);
                default -> handleGenericMessage(message);
            };
            
            if (processed) {
                // 후처리 작업
                postProcessMessage(message);
                
                // 최근 메시지로 기록
                recordRecentMessage(message);
                
                log.debug("채팅 메시지 처리 완료: 방ID={}, 발신자={}, 타입={}", 
                         message.getRoomId(), message.getSenderId(), message.getContentType());
            }
            
            return processed;
            
        } catch (Exception e) {
            log.error("채팅 메시지 처리 실패: ID={}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 텍스트 메시지 처리
     */
    private boolean handleTextMessage(ChatMessage message) {
        textMessages.incrementAndGet();
        
        log.info("📝 텍스트 메시지: 방ID={}, 발신자={}, 내용='{}'", 
                message.getRoomId(), message.getSenderName(), 
                message.getContent().length() > 100 ? 
                    message.getContent().substring(0, 100) + "..." : message.getContent());
        
        // 멘션 처리
        if (message.hasMentions()) {
            handleMentions(message);
        }
        
        // 답장 처리
        if (message.isReply()) {
            handleReply(message);
        }
        
        // 실시간 타이핑 상태 업데이트 (옵션)
        updateUserActivity(message.getRoomId(), message.getSenderId());
        
        return true;
    }
    
    /**
     * 파일 메시지 처리
     */
    private boolean handleFileMessage(ChatMessage message) {
        fileMessages.incrementAndGet();
        
        if (message.hasAttachments()) {
            for (ChatMessage.FileAttachment attachment : message.getAttachments()) {
                log.info("📎 파일 메시지: 방ID={}, 파일명={}, 크기={}bytes, 타입={}", 
                        message.getRoomId(), attachment.getFileName(), 
                        attachment.getFileSize(), attachment.getFileType());
                
                // 파일 크기 검증
                if (attachment.getFileSize() != null && attachment.getFileSize() > 10_000_000) { // 10MB
                    log.warn("파일 크기 초과: {}bytes", attachment.getFileSize());
                    return false;
                }
                
                // 파일 타입 검증 (보안)
                if (!isAllowedFileType(attachment.getFileType())) {
                    log.warn("허용되지 않는 파일 타입: {}", attachment.getFileType());
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 이미지 메시지 처리
     */
    private boolean handleImageMessage(ChatMessage message) {
        log.info("🖼️ 이미지 메시지: 방ID={}, 발신자={}", 
                message.getRoomId(), message.getSenderName());
        
        // 이미지 썸네일 생성 등의 추가 처리 가능
        return handleFileMessage(message);
    }
    
    /**
     * 오디오 메시지 처리
     */
    private boolean handleAudioMessage(ChatMessage message) {
        log.info("🎵 오디오 메시지: 방ID={}, 발신자={}", 
                message.getRoomId(), message.getSenderName());
        
        // 오디오 길이 검증 등
        return handleFileMessage(message);
    }
    
    /**
     * 비디오 메시지 처리
     */
    private boolean handleVideoMessage(ChatMessage message) {
        log.info("🎬 비디오 메시지: 방ID={}, 발신자={}", 
                message.getRoomId(), message.getSenderName());
        
        // 비디오 썸네일 생성 등
        return handleFileMessage(message);
    }
    
    /**
     * 일반 메시지 처리
     */
    private boolean handleGenericMessage(ChatMessage message) {
        log.info("💬 일반 메시지: 방ID={}, 타입={}, 발신자={}", 
                message.getRoomId(), message.getContentType(), message.getSenderName());
        
        return true;
    }
    
    /**
     * 멘션 처리
     */
    private void handleMentions(ChatMessage message) {
        mentionMessages.incrementAndGet();
        
        for (String mentionedUserId : message.getMentionedUserIds()) {
            log.info("👤 멘션 알림: 방ID={}, 멘션된사용자={}, 발신자={}", 
                    message.getRoomId(), mentionedUserId, message.getSenderName());
            
            // 여기서 푸시 알림 또는 이메일 알림을 트리거할 수 있음
            // notificationService.sendMentionNotification(mentionedUserId, message);
        }
    }
    
    /**
     * 답장 처리
     */
    private void handleReply(ChatMessage message) {
        replyMessages.incrementAndGet();
        
        log.info("↩️ 답장 메시지: 방ID={}, 원본메시지ID={}, 답장자={}", 
                message.getRoomId(), message.getReplyToMessageId(), message.getSenderName());
        
        // 원본 메시지 정보를 포함한 알림 처리
        if (message.getQuotedContent() != null) {
            log.debug("답장 내용: '{}' -> '{}'", message.getQuotedContent(), message.getContent());
        }
    }
    
    /**
     * 메시지 후처리
     */
    private void postProcessMessage(ChatMessage message) {
        // 사용자 온라인 상태 업데이트
        updateUserActivity(message.getRoomId(), message.getSenderId());
        
        // 채팅방 마지막 활동 시간 업데이트
        updateRoomActivity(message.getRoomId());
        
        // 읽지 않은 메시지 카운트 증가
        incrementUnreadCount(message.getRoomId(), message.getSenderId());
        
        // 실시간 알림 처리 (웹소켓, SSE 등)
        broadcastToRoomMembers(message);
    }
    
    /**
     * 사용자 활동 업데이트
     */
    private void updateUserActivity(String roomId, String userId) {
        activeUsersInRoom.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
                         .add(userId);
        
        // 일정 시간 후 자동 제거하는 로직 추가 가능
        // scheduleUserActivityCleanup(roomId, userId);
    }
    
    /**
     * 채팅방 활동 업데이트
     */
    private void updateRoomActivity(String roomId) {
        // Redis나 데이터베이스에 마지막 활동 시간 업데이트
        log.debug("채팅방 활동 업데이트: 방ID={}, 시간={}", roomId, LocalDateTime.now());
    }
    
    /**
     * 읽지 않은 메시지 카운트 증가
     */
    private void incrementUnreadCount(String roomId, String senderId) {
        Set<String> activeUsers = activeUsersInRoom.get(roomId);
        if (activeUsers != null) {
            // 발신자를 제외한 다른 사용자들의 읽지 않은 메시지 카운트 증가
            activeUsers.stream()
                     .filter(userId -> !userId.equals(senderId))
                     .forEach(userId -> {
                         // unreadMessageService.increment(userId, roomId);
                         log.debug("읽지 않은 메시지 증가: 사용자={}, 방ID={}", userId, roomId);
                     });
        }
    }
    
    /**
     * 채팅방 멤버들에게 실시간 브로드캐스트
     */
    private void broadcastToRoomMembers(ChatMessage message) {
        // WebSocket이나 Server-Sent Events를 통한 실시간 전송
        log.debug("실시간 브로드캐스트: 방ID={}, 메시지ID={}", 
                 message.getRoomId(), message.getMessageId());
        
        // webSocketService.broadcastToRoom(message.getRoomId(), message);
    }
    
    /**
     * 중복 메시지 체크
     */
    private boolean isDuplicateMessage(ChatMessage message) {
        String messageId = message.getMessageId();
        LocalDateTime lastSeen = recentMessages.get(messageId);
        
        if (lastSeen != null) {
            // 5분 이내에 같은 메시지 ID를 본 경우 중복으로 간주
            return lastSeen.isAfter(LocalDateTime.now().minusMinutes(5));
        }
        
        return false;
    }
    
    /**
     * 최근 메시지 기록
     */
    private void recordRecentMessage(ChatMessage message) {
        recentMessages.put(message.getMessageId(), LocalDateTime.now());
        
        // 메모리 누수 방지를 위해 오래된 항목 정리 (별도 스케줄러에서 수행)
        cleanupOldMessages();
    }
    
    /**
     * 오래된 메시지 기록 정리
     */
    private void cleanupOldMessages() {
        if (recentMessages.size() > 1000) { // 임계치 초과시 정리
            LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
            recentMessages.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        }
    }
    
    /**
     * 메시지 허용 여부 검사 (컨텐츠 필터링)
     */
    private boolean isMessageAllowed(ChatMessage message) {
        String content = message.getContent();
        if (content == null) return true;
        
        // 간단한 금지어 필터링 (실제로는 더 정교한 시스템 필요)
        String[] blockedWords = {"spam", "abuse", "illegal"};
        String lowerContent = content.toLowerCase();
        
        for (String blocked : blockedWords) {
            if (lowerContent.contains(blocked)) {
                return false;
            }
        }
        
        // 스팸 메시지 체크 (동일한 내용 반복 전송 등)
        if (isSpamMessage(message)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 스팸 메시지 체크
     */
    private boolean isSpamMessage(ChatMessage message) {
        // 간단한 스팸 검증 로직
        // 실제로는 더 복잡한 ML 기반 분류기나 규칙 엔진을 사용
        
        String content = message.getContent();
        if (content == null) return false;
        
        // 같은 내용 반복 (길이가 짧고 반복되는 경우)
        if (content.length() < 10 && content.matches("(.)\\1{5,}")) {
            return true;
        }
        
        // URL이 너무 많은 경우
        long urlCount = content.toLowerCase().split("http").length - 1;
        if (urlCount > 3) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 허용된 파일 타입 체크
     */
    private boolean isAllowedFileType(String fileType) {
        if (fileType == null) return false;
        
        return switch (fileType.toUpperCase()) {
            case "IMAGE", "DOCUMENT", "PDF", "AUDIO", "VIDEO", "SPREADSHEET" -> true;
            case "EXECUTABLE", "SCRIPT", "UNKNOWN" -> false;
            default -> true; // 기본적으로 허용
        };
    }
    
    /**
     * 핸들러 통계 조회
     */
    public ChatHandlerStats getStats() {
        return ChatHandlerStats.builder()
                .totalProcessed(totalProcessed.get())
                .textMessages(textMessages.get())
                .fileMessages(fileMessages.get())
                .mentionMessages(mentionMessages.get())
                .replyMessages(replyMessages.get())
                .activeRooms(activeUsersInRoom.size())
                .build();
    }
    
    /**
     * 채팅 핸들러 통계 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class ChatHandlerStats {
        private final long totalProcessed;
        private final long textMessages;
        private final long fileMessages;
        private final long mentionMessages;
        private final long replyMessages;
        private final int activeRooms;
        
        public double getSuccessRate() {
            return totalProcessed > 0 ? 100.0 : 0.0; // 간단한 구현
        }
    }
    
    // Set 타입을 위한 import 추가를 위한 참조
    private java.util.Set<String> createSet() {
        return java.util.concurrent.ConcurrentHashMap.newKeySet();
    }
}