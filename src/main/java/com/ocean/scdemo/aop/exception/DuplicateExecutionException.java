package com.ocean.scdemo.aop.exception;

/**
 * 중복 실행 방지 위반 시 발생하는 예외
 */
public class DuplicateExecutionException extends RuntimeException {

    private final String lockKey;

    public DuplicateExecutionException(String message) {
        super(message);
        this.lockKey = null;
    }

    public DuplicateExecutionException(String message, String lockKey) {
        super(message);
        this.lockKey = lockKey;
    }

    public DuplicateExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.lockKey = null;
    }

    public String getLockKey() {
        return lockKey;
    }

    @Override
    public String toString() {
        return "DuplicateExecutionException{" +
                "message='" + getMessage() + '\'' +
                ", lockKey='" + lockKey + '\'' +
                '}';
    }
}
