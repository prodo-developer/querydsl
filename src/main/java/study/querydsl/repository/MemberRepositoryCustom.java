package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

/**
 * 사용자 정의 리포지토리 사용법
 * 1. 사용자 정의 인터페이스 작성 (MemberRepositoryCustom)
 * 2. 사용자 정의 인터페이스 구현 (MemberRepositoryImpl)
 * 3. 스프링 데이터 리포지토리에 사용자 정의 인터페이스 상속
 */
public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);

    // 단순 페이징 처리 (전체 카운트를 한번에 조회하는 단순한 방법)
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);

    // 복잡한 페이징 처리 (데이터 내용과 전체 카운트를 별도로 조회하는 방법)
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
