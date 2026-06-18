package com.exercise.bookmateserver.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 및 로그인 API")
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "이메일 회원가입", description = "이메일, 비밀번호로 회원가입합니다. 닉네임이 비어 있으면 서버에서 자동 생성합니다.")
    @PostMapping("/api/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "랜덤 닉네임 추천", description = "아직 사용 중이지 않은 랜덤 닉네임을 추천합니다.")
    @GetMapping("/api/auth/nickname-suggestion")
    public NicknameSuggestionResponse suggestNickname() {
        return authService.suggestNickname();
    }

    @Operation(summary = "이메일 중복 확인", description = "이메일 형식과 가입 가능 여부를 확인합니다.")
    @GetMapping("/api/auth/email-availability")
    public EmailAvailabilityResponse checkEmailAvailability(@RequestParam String email) {
        return authService.checkEmailAvailability(email);
    }

    @Operation(summary = "닉네임 중복 확인", description = "닉네임 길이와 중복 여부를 확인합니다.")
    @GetMapping("/api/auth/nickname-availability")
    public NicknameAvailabilityResponse checkNicknameAvailability(@RequestParam String nickname) {
        return authService.checkNicknameAvailability(nickname);
    }

    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "카카오 로그인", description = "카카오 access token으로 로그인하거나 자동 회원가입합니다.")
    @PostMapping("/api/auth/kakao")
    public AuthResponse kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return authService.loginWithKakao(request);
    }

    @Operation(summary = "로그아웃", description = "현재 서버 토큰은 stateless 방식이므로 클라이언트에서 토큰을 삭제하면 로그아웃됩니다.")
    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
    }
}
