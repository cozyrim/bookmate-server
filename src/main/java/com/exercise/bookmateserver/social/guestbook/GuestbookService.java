package com.exercise.bookmateserver.social.guestbook;

import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GuestbookService {

    private final GuestbookRepository guestbookRepository;
    private final UserRepository userRepository;

    public GuestbookService(GuestbookRepository guestbookRepository, UserRepository userRepository) {
        this.guestbookRepository = guestbookRepository;
        this.userRepository = userRepository;
    }

    public List<GuestbookMessageResponse> getMessages(UUID targetUserId) {
        UserEntity targetUser = userRepository.findByIdAndIsPublicTrue(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공개된 사용자를 찾을 수 없습니다."));

        return guestbookRepository.findAllByTargetUserIdOrderByCreatedAtDesc(targetUser.getId())
                .stream()
                .map(GuestbookMessageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public GuestbookMessageResponse writeMessage(UserEntity writer, UUID targetUserId, GuestbookWriteRequest request) {
        UserEntity targetUser = userRepository.findByIdAndIsPublicTrue(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "방명록을 남길 수 없는 사용자입니다."));

        GuestbookEntity message = new GuestbookEntity(targetUser, writer, request.content());
        GuestbookEntity savedMessage = guestbookRepository.save(message);

        return GuestbookMessageResponse.from(savedMessage);
    }

    @Transactional
    public void deleteMessage(UserEntity currentUser, UUID messageId) {
        GuestbookEntity message = guestbookRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "방명록 메시지를 찾을 수 없습니다."));

        boolean isWriter = message.getWriterUser().getId().equals(currentUser.getId());
        boolean isOwner = message.getTargetUser().getId().equals(currentUser.getId());

        if (!isWriter && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        guestbookRepository.delete(message);
    }
}
