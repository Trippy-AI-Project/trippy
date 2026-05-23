package pse.trippy.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.userservice.dto.request.UpdateProfileRequest;
import pse.trippy.userservice.exception.UserNotFoundException;
import pse.trippy.userservice.model.dto.UserProfileDto;
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.model.enums.UserRole;
import pse.trippy.userservice.repository.UserRepository;

import java.util.UUID;

/**
 * Service handling user profile operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Returns the profile for the given user ID.
     *
     * @param userId the authenticated user's ID
     * @return a populated {@link UserProfileDto}
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return toProfileDto(user);
    }

    /**
     * Applies a partial update to the authenticated user's profile.
     * Only non-null fields in the request are applied.
     *
     * @param userId  the authenticated user's ID
     * @param request the fields to update
     * @return the updated profile
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional
    public UserProfileDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User saved = userRepository.save(user);
        log.info("Profile updated for userId={}", userId);
        return toProfileDto(saved);
    }

    /**
     * Upgrades a user's platform role to HOST when they create their first trip.
     * No-op if the user is already a HOST or ADMIN.
     *
     * @param userId the user to promote
     */
    @Transactional
    public void upgradeToHost(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (user.getRole() == UserRole.MEMBER || user.getRole() == UserRole.USER) {
            user.setRole(UserRole.HOST);
            userRepository.save(user);
            log.info("User {} upgraded to HOST role", userId);
        }
    }

    private UserProfileDto toProfileDto(User user) {
        return UserProfileDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .role(user.getRole())
                .build();
    }
}
