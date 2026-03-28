package pse.trippy.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.userservice.model.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Returns the user with the given email address, if one exists.
     *
     * @param email the email to look up (case-sensitive)
     * @return an {@link Optional} containing the matching user, or empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns {@code true} when a user with the given email is already registered.
     *
     * @param email the email to check
     * @return {@code true} if the email is taken
     */
    boolean existsByEmail(String email);
}
