package com.telecamnig.cinemapos.service;

import org.springframework.http.ResponseEntity;

import com.telecamnig.cinemapos.dto.CommonApiResponse;
import com.telecamnig.cinemapos.dto.UserResponse;
import com.telecamnig.cinemapos.dto.UsersResponse;

public interface UserService {

    ResponseEntity<UserResponse> getUserByPublicId(String publicUserId);
	
	ResponseEntity<UsersResponse> getUsersByRoleAndStatus(String role, Integer status);
	
	ResponseEntity<CommonApiResponse> updateUserStatus(String publicUserId, Integer status);

}
