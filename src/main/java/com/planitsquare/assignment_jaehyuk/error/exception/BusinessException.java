package com.planitsquare.assignment_jaehyuk.error.exception;

import com.planitsquare.assignment_jaehyuk.error.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }
}
