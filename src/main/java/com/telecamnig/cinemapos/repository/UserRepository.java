package com.telecamnig.cinemapos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telecamnig.cinemapos.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== BASIC FINDERS ==========
    
    /**
     * Find user by external userId (UUID).
     * Indexed: idx_users_userid
     */
    Optional<User> findByUserId(String userId);

    /**
     * Find user by email (normalized to lowercase).
     * Indexed: idx_users_email
     */
    Optional<User> findByEmailId(String emailId);

    /**
     * Find user by contact number.
     * Indexed: idx_users_contact
     */
    Optional<User> findByContactNo(String contactNo);

    // ========== EXISTS CHECKS ==========
    
    /**
     * Check if email exists (for validation during registration).
     */
    boolean existsByEmailId(String emailId);

    /**
     * Check if contact number exists.
     */
    boolean existsByContactNo(String contactNo);

    /**
     * Check if userId exists.
     */
    boolean existsByUserId(String userId);

    // ========== STATUS-BASED FILTERS ==========
    
    /**
     * Find all active users (status = ACTIVE).
     */
    List<User> findByStatus(int status);

    /**
     * Find user by email and status.
     */
    Optional<User> findByEmailIdAndStatus(String emailId, int status);

    /**
     * Find user by contact and status.
     */
    Optional<User> findByContactNoAndStatus(String contactNo, int status);

    // ========== ROLE-BASED QUERIES ==========
    
    /**
     * Find users by role name with JOIN FETCH to avoid N+1.
     * Uses JOIN FETCH to load roles in same query.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * Find users by role name and status with JOIN FETCH.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.roles r WHERE r.name = :roleName AND u.status = :status")
    List<User> findByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") int status);

    /**
     * Find all counter staff (active users with COUNTER role).
     */
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.roles r WHERE r.name = 'ROLE_COUNTER' AND u.status = 1")
    List<User> findAllActiveCounterStaff();

    /**
     * Find all admins (active users with ADMIN role).
     */
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.roles r WHERE r.name = 'ROLE_ADMIN' AND u.status = 1")
    List<User> findAllActiveAdmins();

    // ========== BULK OPERATIONS ==========
    
    /**
     * Count users by status.
     */
    long countByStatus(int status);

    /**
     * Count users by role.
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    // ========== VALIDATION QUERIES ==========
    
    /**
     * Check if email exists excluding a specific user (for updates).
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.emailId = :email AND u.userId != :excludeUserId")
    boolean existsByEmailIdExcludingUser(@Param("email") String emailId, 
                                       @Param("excludeUserId") String excludeUserId);

    /**
     * Check if contact exists excluding a specific user.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.contactNo = :contact AND u.userId != :excludeUserId")
    boolean existsByContactNoExcludingUser(@Param("contact") String contactNo, 
                                         @Param("excludeUserId") String excludeUserId);

    // ========== PERFORMANCE OPTIMIZED ==========
    
    /**
     * Find user with roles by userId (single query with fetch).
     */
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.userId = :userId")
    Optional<User> findByUserIdWithRoles(@Param("userId") String userId);

    /**
     * Find user with roles by email (single query with fetch).
     */
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.emailId = :emailId")
    Optional<User> findByEmailIdWithRoles(@Param("emailId") String emailId);
    
}