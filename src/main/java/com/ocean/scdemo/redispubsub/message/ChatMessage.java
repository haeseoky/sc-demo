package com.ocean.scdemo.redispubsub.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * 채팅 메시지 모델
 * 
 * 실시간 채팅, 그룹 채팅, 1:1 대화 등을 지원
 * 파일 첨부, 멘션, 답장 기능 포함
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatMessage extends BaseMessage {
    
    /**
     * 채팅방 정보
     */
    private String roomId;
    private String roomName;
    private String roomType; // GROUP, DIRECT, PUBLIC, PRIVATE
    
    /**
     * 메시지 내용
     */
    private String content;
    private String contentType; // TEXT, IMAGE, FILE, AUDIO, VIDEO, EMOJI
    
    /**
     * 수신자 정보 (1:1 채팅 또는 멘션)
     */
    private String recipientId;
    private List<String> mentionedUserIds;
    
    /**
     * 답장 기능
     */
    private String replyToMessageId;
    private String quotedContent;
    
    /**
     * 파일 첨부
     */
    private List<FileAttachment> attachments;
    
    /**
     * 메시지 상태
     */
    private String status; // SENT, DELIVERED, READ, FAILED
    private boolean edited;
    private boolean deleted;
    
    /**
     * 읽음 확인
     */
    private List<ReadReceipt> readReceipts;
    
    /**
     * 반응/이모지
     */
    private Map<String, List<String>> reactions; // emoji -> userIds
    
    /**
     * 메시지 포맷팅
     */
    private boolean formatted; // 마크다운, HTML 등
    private String formatType;
    
    /**
     * 지역 정보 (선택사항)
     */
    private LocationInfo location;
    
    /**
     * 메시지 생성자 편의 메서드
     */
    public static ChatMessage createTextMessage(String senderId, String senderName, 
                                               String roomId, String content) {
        return ChatMessage.builder()
                .messageType("CHAT")
                .senderId(senderId)
                .senderName(senderName)
                .roomId(roomId)
                .content(content)
                .contentType("TEXT")
                .status("SENT")
                .build();
    }
    
    public static ChatMessage createFileMessage(String senderId, String senderName,
                                               String roomId, String fileName, String fileUrl) {
        FileAttachment attachment = FileAttachment.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileType(getFileType(fileName))
                .build();
                
        return ChatMessage.builder()
                .messageType("CHAT")
                .senderId(senderId)
                .senderName(senderName)
                .roomId(roomId)
                .content("파일을 전송했습니다: " + fileName)
                .contentType("FILE")
                .attachments(List.of(attachment))
                .status("SENT")
                .build();
    }
    
    /**
     * 멘션이 있는지 확인
     */
    public boolean hasMentions() {
        return mentionedUserIds != null && !mentionedUserIds.isEmpty();
    }
    
    /**
     * 특정 사용자가 멘션되었는지 확인
     */
    public boolean isMentioned(String userId) {
        return mentionedUserIds != null && mentionedUserIds.contains(userId);
    }
    
    /**
     * 답장 메시지인지 확인
     */
    public boolean isReply() {
        return replyToMessageId != null;
    }
    
    /**
     * 파일 첨부가 있는지 확인
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
    
    /**
     * 반응 추가
     */
    public void addReaction(String emoji, String userId) {
        if (reactions == null) {
            reactions = new java.util.HashMap<>();
        }
        reactions.computeIfAbsent(emoji, k -> new java.util.ArrayList<>()).add(userId);
    }
    
    /**
     * 읽음 확인 추가
     */
    public void addReadReceipt(String userId) {
        if (readReceipts == null) {
            readReceipts = new java.util.ArrayList<>();
        }
        readReceipts.add(ReadReceipt.builder()
                .userId(userId)
                .readAt(java.time.LocalDateTime.now())
                .build());
    }
    
    @Override
    public BaseMessage copy() {
        return ChatMessage.builder()
                .messageId(this.getMessageId())
                .messageType(this.getMessageType())
                .senderId(this.getSenderId())
                .senderName(this.getSenderName())
                .channel(this.getChannel())
                .topic(this.getTopic())
                .timestamp(this.getTimestamp())
                .priority(this.getPriority())
                .ttl(this.getTtl())
                .metadata(this.getMetadata() != null ? new java.util.HashMap<>(this.getMetadata()) : null)
                .roomId(this.roomId)
                .roomName(this.roomName)
                .roomType(this.roomType)
                .content(this.content)
                .contentType(this.contentType)
                .recipientId(this.recipientId)
                .mentionedUserIds(this.mentionedUserIds != null ? new java.util.ArrayList<>(this.mentionedUserIds) : null)
                .replyToMessageId(this.replyToMessageId)
                .quotedContent(this.quotedContent)
                .attachments(this.attachments != null ? new java.util.ArrayList<>(this.attachments) : null)
                .status(this.status)
                .edited(this.edited)
                .deleted(this.deleted)
                .readReceipts(this.readReceipts != null ? new java.util.ArrayList<>(this.readReceipts) : null)
                .reactions(this.reactions != null ? new java.util.HashMap<>(this.reactions) : null)
                .formatted(this.formatted)
                .formatType(this.formatType)
                .location(this.location)
                .build();
    }
    
    /**
     * 파일 타입 추론
     */
    private static String getFileType(String fileName) {
        if (fileName == null) return "UNKNOWN";
        
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "webp" -> "IMAGE";
            case "mp4", "avi", "mov", "wmv" -> "VIDEO";
            case "mp3", "wav", "m4a" -> "AUDIO";
            case "pdf" -> "PDF";
            case "doc", "docx" -> "DOCUMENT";
            case "xls", "xlsx" -> "SPREADSHEET";
            default -> "FILE";
        };
    }
    
    /**
     * 파일 첨부 정보
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileAttachment {
        private String fileName;
        private String fileUrl;
        private String fileType;
        private Long fileSize;
        private String mimeType;
        private String thumbnailUrl; // 이미지/비디오용
        private Integer duration; // 오디오/비디오용 (초)
        private Map<String, Object> metadata;
    }
    
    /**
     * 읽음 확인 정보
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadReceipt {
        private String userId;
        private String userName;
        private java.time.LocalDateTime readAt;
    }
    
    /**
     * 위치 정보
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
        private String address;
        private String placeName;
        private Double accuracy;
    }
}