package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.exception.DuplicateEmailException;
import com.modernizedkitechensink.kitchensinkmodernized.exception.MemberNotFoundException;
import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements IMemberService {

  private final MemberRepository memberRepository;

  @Override
  public List<Member> findAll() {
    return memberRepository.findAllByOrderByNameAsc();
  }

  @Override
  public Member findById(String id) {
    return memberRepository.findById(id)
      .orElseThrow(() -> new MemberNotFoundException(id));
  }

  @Override
  public Member register(Member member) {
    // Check for duplicate email
    if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
      throw new DuplicateEmailException(member.getEmail());
    }
    return memberRepository.save(member);
  }
}