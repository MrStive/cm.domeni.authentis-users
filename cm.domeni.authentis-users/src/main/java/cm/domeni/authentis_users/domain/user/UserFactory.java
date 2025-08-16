package cm.domeni.authentis_users.domain.user;

public interface UserFactory {
  User create(UserId id, UserData data);
}
