package cm.domeni.authentis_users.api;

import cm.domeni.authentis_users.dto.RefreshTokenRequest;
import cm.domeni.authentis_users.dto.RefreshTokenResponse;
import cm.domeni.authentis_users.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthResource implements AuthApi {
  private final AuthService authService;

  @Override
  public ResponseEntity<RefreshTokenResponse> refreshToken(
      RefreshTokenRequest refreshTokenRequest) {
    return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest));
  }

  @Override
  public ResponseEntity<Void> logout(RefreshTokenRequest refreshTokenRequest) {
    authService.logout(refreshTokenRequest);
    return ResponseEntity.noContent().build();
  }
}
