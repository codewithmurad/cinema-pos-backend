package com.telecamnig.cinemapos.service;

import org.springframework.http.ResponseEntity;

import com.telecamnig.cinemapos.dto.AdminRegisterRequest;
import com.telecamnig.cinemapos.dto.ChangePasswordRequest;
import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.CounterRegisterRequest;
import com.telecamnig.cinemapos.dto.ForgotPasswordRequest;
import com.telecamnig.cinemapos.dto.LoginRequest;
import com.telecamnig.cinemapos.dto.LoginResponse;

public interface AuthService {

	ResponseEntity<LoginResponse> login(LoginRequest request);

	ResponseEntity<CommonApiResponse> registerAdmin(AdminRegisterRequest request);

	ResponseEntity<CommonApiResponse> registerCounter(CounterRegisterRequest request);
	
	ResponseEntity<CommonApiResponse> forgotPassword(ForgotPasswordRequest request);
	
	ResponseEntity<CommonApiResponse> changePassword(ChangePasswordRequest request);
	
}
