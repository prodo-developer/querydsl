package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;
import study.querydsl.repository.MemberTestRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;
    private final MemberTestRepository memberTestRepository;

    // 조회 API 컨트롤러 개발
    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepository.search(condition);
    }

    // 단순 페이징 처리 (전체 카운트를 한번에 조회하는 단순한 방법)
    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageSimple(condition, pageable);
    }

    // 복잡한 페이징 처리 (데이터 내용과 전체 카운트를 별도로 조회하는 방법)
    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplex(condition, pageable);
    }

    // Querydsl 지원 클래스 직접 만들어서 페이징 처리 구현
    @GetMapping("/v4/members")
    public Page<Member> searchMemberV4(MemberSearchCondition condition, Pageable pageable) {
        return memberTestRepository.applyPagination(condition, pageable);
    }

    // Querydsl 지원 클래스 직접 만들어서 페이징 처리 구현2
    @GetMapping("/v5/members")
    public Page<Member> searchMemberV5(MemberSearchCondition condition, Pageable pageable) {
        return memberTestRepository.applyPagination2(condition, pageable);
    }
}
