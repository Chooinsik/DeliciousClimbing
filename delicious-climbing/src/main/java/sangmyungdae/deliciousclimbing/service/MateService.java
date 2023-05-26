package sangmyungdae.deliciousclimbing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sangmyungdae.deliciousclimbing.dto.mate.*;
import sangmyungdae.deliciousclimbing.domain.entity.*;
import sangmyungdae.deliciousclimbing.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MateService {

    private final MateRepository mateRepository;
    private final MateCommentRepository commentRepository;
    private final MateFileRepository fileRepository;
    private final MountainRepository mountainRepository;
    private final UserRepository userRepository;

    // 게시글 목록 조회 → Pagination 구현
    @Transactional
    public Page<MatePost> getMateList(MateSearchDto dto, Pageable pageable) {

        Page<TbMate> findMates;
        if (dto.getMountainId() == null && dto.getRecruitStatusFiltering() == false) {
            // 모든 글 반환
            findMates = mateRepository.findAll(pageable);
        } else if (dto.getMountainId() == null && dto.getRecruitStatusFiltering() == true) {
            // 산 필터링 X, 모집중인 게시글만 보기 O
            findMates = mateRepository.findPageByRecruitStatus(dto.getRecruitStatusFiltering(), pageable);
        } else if (dto.getMountainId() != null && dto.getRecruitStatusFiltering() == false) {
            // 산 필터링 O, 모집중인 게시글만 보기 X
            findMates = mateRepository.findPageByMountain_Id(dto.getMountainId(), pageable);
        } else {
            // 산 필터링 O, 모집중인 게시글만 보기 O
            findMates = mateRepository.findPageByMountain_IdAndRecruitStatus(dto.getMountainId(), dto.getRecruitStatusFiltering(), pageable);
        }

        return MatePost.toDtoList(findMates);
    }

    // 게시글 상세 조회
    @Transactional
    public MatePost getPostDetail(Long mateId) {

        TbMate tbMate = mateRepository.findById(mateId).orElseThrow(() -> new IllegalArgumentException("tbMate doesn't exist. mateId=" + mateId));

        return MatePost.builder()
                .entity(tbMate)
                .build();
    }

    // 게시글 생성
    @Transactional
    public Mate createPost(Long userId, MateDto dto) {

        TbMountain findMountain = mountainRepository.getReferenceById(dto.getMountainId());
        TbUser findUser = userRepository.getReferenceById(userId);

        TbMate entity = mateRepository.save(dto.toEntity(findUser, findMountain));

        return Mate.builder()
                .entity(entity)
                .build();
    }

    // 게시글 수정
    @Transactional
    public Mate updatePost(Long mateId, MateDto dto) {

        TbMate tbMate = mateRepository.findById(mateId).orElseThrow(() -> new IllegalArgumentException("tbMate doesn't exist. mateId=" + mateId));
        tbMate.update(dto.getTitle(), dto.getContent(), dto.getRecruitCount(), dto.getRecruitStatus(), dto.getRecruitDate());

        return Mate.builder()
                .entity(tbMate)
                .build();
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long mateId) {

        TbMate findMate = mateRepository.findById(mateId).orElseThrow(() -> new IllegalArgumentException("tbMate doesn't exist. mateId=" + mateId));
        List<Long> commentIds = findMate.getMateComments().stream().map(comment -> comment.getId()).collect(Collectors.toList());
        commentIds.stream().forEach(id -> deleteComment(findMate.getId(), id));
        List<Long> fileIds = findMate.getMateFiles().stream().map(file -> file.getId()).collect(Collectors.toList());
        fileIds.stream().forEach(id -> deleteFile(findMate.getId(), id));
        mateRepository.deleteById(mateId);
    }

    // 댓글 생성
    @Transactional
    public MateComment createComment(Long mateId, Long userId, MateCommentDto dto) {

        // Request DTO to Entity
        TbMate findMate = mateRepository.getReferenceById(mateId);
        TbUser findUser = userRepository.getReferenceById(userId);


        TbMateComment entity = commentRepository.save(dto.toEntity(findMate, findUser));
        findMate.getMateComments().add(entity);

        // Entity to Response DTO
        return MateComment.builder()
                .entity(entity)
                .build();
    }

    // 댓글 수정
    @Transactional
    public MateComment updateComment(Long mateCommentId, MateCommentDto dto) {

        TbMateComment entity = commentRepository.findById(mateCommentId).orElseThrow(() -> new IllegalArgumentException("tbMateComment doesn't exist. mateCommentId=" + mateCommentId));
        entity.update(dto.getContent());

        return MateComment.builder()
                .entity(entity)
                .build();
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long mateId, Long commentId) {

        mateRepository.getReferenceById(mateId)
                .getMateComments()
                .remove(commentRepository.getReferenceById(commentId));

        commentRepository.deleteById(commentId);
    }


    // 파일 생성
    @Transactional
    public MateFile createFile(Long mateId, MateFileDto dto) {

        TbMate findMate = mateRepository.getReferenceById(mateId);

        // Request Dto to Entity
        TbMateFile entity = fileRepository.save(dto.toEntity(findMate));
        findMate.getMateFiles().add(entity);


        // Entity to Response DTO
        return MateFile.builder()
                .entity(entity)
                .build();
    }

    // 파일 수정
    @Transactional
    public MateFile updateFile(Long fileId, MateFileDto dto) {

        TbMateFile entity = fileRepository.findById(fileId).orElseThrow(() -> new IllegalArgumentException("tbMateFile doesn't exist. fileId=" + fileId));
        entity.update(dto.getFileName());

        return MateFile.builder()
                .entity(entity)
                .build();
    }

    // 파일 삭제
    @Transactional
    public void deleteFile(Long mateId, Long fileId) {

        mateRepository.getReferenceById(mateId)
                .getMateFiles()
                .remove(fileRepository.getReferenceById(fileId));

        fileRepository.deleteById(fileId);
    }
}