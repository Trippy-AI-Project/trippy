package pse.trippy.userservice.mapper;

import org.mapstruct.Mapper;
import pse.trippy.userservice.dto.response.UserProfileResponse;
import pse.trippy.userservice.model.entity.User;

/**
 * MapStruct mapper for {@link User} entity to profile DTOs.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps a {@link User} entity to a {@link UserProfileResponse}.
     * All field names match, so no explicit {@code @Mapping} annotations are required.
     */
    UserProfileResponse toResponse(User user);
}
