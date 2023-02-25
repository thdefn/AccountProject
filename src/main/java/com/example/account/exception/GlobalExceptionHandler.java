package com.example.account.exception;

import com.example.account.dto.ErrorResponse;
import com.example.account.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccountException.class)
    public ErrorResponse handleAccountException(AccountException e){
        log.error("{} is occurred",e.getErrorCode());
        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage()); //두개밖에 없으므로 빌더 쓰지 않음
    }

    @ExceptionHandler(TransactionException.class)
    public ErrorResponse handleTransactionException(TransactionException e){
        log.error("{} is occurred",e.getErrorCode());
        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage()); //두개밖에 없으므로 빌더 쓰지 않음
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        //에러 응답을 명확하게 내릴 수 있다
        log.error("DataIntegrityViolationException is occurred",e);
        return new ErrorResponse(ErrorCode.INVALID_REQUEST,
                ErrorCode.INVALID_REQUEST.getDescription());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException e){
        //상대적으로 자주 발생하는 에러
        //db 테이블에 유니크 키가 있는데 그 키가 중복되어서 저장하려고 할때
        log.error("DataIntegrityViolationException is occurred",e);
        return new ErrorResponse(ErrorCode.INVALID_REQUEST,
                ErrorCode.INVALID_REQUEST.getDescription());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e){ //가장 최종적으로 발생하는 모든 익셉션에 대한 처리가 무조건 있어야 한다
        log.error("Exception is occurred",e);
        return new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getDescription());
    }
}
