package pse.trippy.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.trippy.userservice.dto.request.UpdateProfileRequest;
import pse.trippy.userservice.dto.response.UserProfileResponse;
import pse.trippy.userservice.exception.UserNotFoundException;
import pse.trippy.userservice.mapper.UserMapper;
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;
import pse.trippy.userservice.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserProfileService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService")
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserProfileService userProfileService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash("$2a$12$hash")
                .displayName("John Doe")
                .bio("Travel enthusiast")
                .phoneNumber("+49123456789")
                .role(UserRole.USER)
                .plan(SubscriptionPlan.FREE)
                .emailVerified(false)
                .createdAt(Instant.parse("2026-03-28T09:00:00Z"))
                .updatedAt(Instant.parse("2026-03-28T09:00:00Z"))
                .build();
    }

    // =========================================================================
    // getProfile()
    // =========================================================================

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("returns mapped profile for existing user")
        void returnsMappedProfile() {
            UserProfileResponse expected = UserProfileResponse.builder()
                    .id(userId)
                    .email("user@example.com")
                    .displayName("John Doe")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(expected);

            UserProfileResponse result = userProfileService.getProfile(userId);

            assertThat(result).isSameAs(expected);
            verify(userRepository).findById(userId);
            verify(userMapper).toResponse(user);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userProfileService.getProfile(userId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(userId.toString());
        }
    }

    // =========================================================================
    // updateProfile()
    // =========================================================================

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("updates all provided non-null fields and saves")
        void updatesProvidedFields() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .displayName("Jane Doe")
                    .bio("New bio")
                    .phoneNumber("+49000000000")
                    .build();

            UserProfileResponse expected = UserProfileResponse.builder()
                    .id(userId)
                    .displayName("Jane Doe")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(expected);

            UserProfileResponse result = userProfileService.updateProfile(userId, request);

            assertThat(result).isSameAs(expected);
            assertThat(user.getDisplayName()).isEqualTo("Jane Doe");
            assertThat(user.getBio()).isEqualTo("New bio");
            assertThat(user.getPhoneNumber()).isEqualTo("+49000000000");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("skips null fields and does not overwrite existing values")
        void skipsNullFields() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .displayName("Jane Doe")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(UserProfileResponse.builder().build());

            userProfileService.updateProfile(userId, request);

            assertThat(user.getDisplayName()).isEqualTo("Jane Doe");
            assertThat(user.getBio()).isEqualTo("Travel enthusiast");
            assertThat(user.getPhoneNumber()).isEqualTo("+49123456789");
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userProfileService.updateProfile(userId, new UpdateProfileRequest()))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
