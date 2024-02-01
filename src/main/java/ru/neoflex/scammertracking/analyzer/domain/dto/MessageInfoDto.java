package ru.neoflex.scammertracking.analyzer.domain.dto;

public class MessageInfoDto {

    public MessageInfoDto(Integer errorCode, Integer respCode, String message) {
        this.errorCode = errorCode;
        this.respCode = respCode;
        this.message = message;
    }

    public MessageInfoDto(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public MessageInfoDto() {
    }

    private Integer errorCode;
    private Integer respCode;
    private String message;

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public Integer getRespCode() {
        return respCode;
    }

    public void setRespCode(Integer respCode) {
        this.respCode = respCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

//    private String timestamp;
//    private int status;
//    private String error;
//    private String message;
//    private String path;
//
//    public String getTimestamp() {
//        return timestamp;
//    }
//
//    public void setTimestamp(String timestamp) {
//        this.timestamp = timestamp;
//    }
//
//    public int getStatus() {
//        return status;
//    }
//
//    public void setStatus(int status) {
//        this.status = status;
//    }
//
//    public String getError() {
//        return error;
//    }
//
//    public void setError(String error) {
//        this.error = error;
//    }
//
//    public String getMessage() {
//        return message;
//    }
//
//    public void setMessage(String message) {
//        this.message = message;
//    }
//
//    public String getPath() {
//        return path;
//    }
//
//    public void setPath(String path) {
//        this.path = path;
//    }
//
//    @Override
//    public String toString() {
//        return "ExceptionMessage [timestamp=" + timestamp + ", status=" + status + ", error=" + error + ", message=" + message + ", path=" + path + "]";
//    }

}