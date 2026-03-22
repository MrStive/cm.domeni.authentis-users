package cm.domeni.authentis_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserData;
import cm.domeni.authentis_users.domain.user.UserFactory;
import cm.domeni.authentis_users.domain.user.UserFetcher;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserName;
import cm.domeni.authentis_users.domain.user.UserUpdater;
import cm.domeni.authentis_users.dto.CreateUser;
import cm.domeni.authentis_users.exception.UserCanNotCreateException;
import cm.domeni.authentis_users.service.mapper.UserMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock private UserFactory userFactory;
  @Mock private UserFetcher userFetcher;
  @Mock private UserUpdater userUpdater;
  @Mock private UserMapper userMapper;
  @Mock private UserOutboxService userOutboxService;

  @Test
  void shouldEnqueueOutboxWhenUserCreated() {
    UserService userService =
        new UserService(userFactory, userFetcher, userUpdater, userMapper, userOutboxService);
    CreateUser request = new CreateUser();
    UserData userData = UserData.builder().build();
    String userId = "11111111-1111-1111-1111-111111111111";
    User createdUser = User.builder().id(new UserId(userId)).build();
    createdUser.setUserName(new UserName("john"));

    when(userMapper.map(request)).thenReturn(userData);
    when(userFactory.create(userData)).thenReturn(createdUser);

    UUID returnedId = userService.createUser(request);

    assertThat(returnedId).isEqualTo(UUID.fromString(userId));
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userOutboxService).enqueueUserCreated(captor.capture());
    User captured = captor.getValue();
    assertThat(captured.getId().toUuid()).isEqualTo(UUID.fromString(userId));
    assertThat(captured.getUserName().getValue()).isEqualTo("john");
  }

  @Test
  void shouldNotPublishEventWhenUserCreationFails() {
    UserService userService =
        new UserService(userFactory, userFetcher, userUpdater, userMapper, userOutboxService);
    CreateUser request = new CreateUser();
    UserData userData = UserData.builder().build();

    when(userMapper.map(request)).thenReturn(userData);
    when(userFactory.create(userData)).thenThrow(new UserCanNotCreateException("failure"));

    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOf(UserCanNotCreateException.class);
    verifyNoInteractions(userOutboxService);
  }
}
