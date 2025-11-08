package com.telecamnig.cinemapos.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_userid", columnList = "userId"),
        @Index(name = "idx_users_email", columnList = "emailId"),
        @Index(name = "idx_users_contact", columnList = "contactNo")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_users_email", columnNames = {"emailId"}),
        @UniqueConstraint(name = "uc_users_userid", columnNames = {"userId"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 36)
    private String userId;  // public id -  UUID

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format")
    @Column(length = 20)
    private String contactNo;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String emailId;             // used for login (normalized)

    @JsonIgnore
    @NotBlank
    @Size(min = 60, max = 100)        // bcrypt hashes ~60 chars; leave margin
    @Column(nullable = false, length = 100)
    private String password;            // bcrypt hash

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private int status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private Long createdById;

    /**
     * Optimistic locking to prevent lost updates in concurrent edits.
     */
    @Version
    private Long version;

    /* --- lifecycle hooks --- */

    @PrePersist
    public void prePersist() {
        if (this.userId == null) {
            this.userId = UUID.randomUUID().toString();
        }
        // normalize email to lower-case
        if (this.emailId != null) {
            this.emailId = this.emailId.trim().toLowerCase();
        }
        if (this.contactNo != null) {
            this.contactNo = this.contactNo.trim();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.emailId != null) {
            this.emailId = this.emailId.trim().toLowerCase();
        }
        if (this.contactNo != null) {
            this.contactNo = this.contactNo.trim();
        }
    }
}
