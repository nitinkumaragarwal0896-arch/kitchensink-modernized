package com.modernizedkitechensink.kitchensinkmodernized.controller;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.service.IMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")  // Added v1 for versioning
@RequiredArgsConstructor
public class MemberController {

  private final IMemberService memberService;

  @GetMapping
  public List<Member> getAllMembers() {
    return memberService.findAll();
  }

  @GetMapping("/{id}")
  public Member getMemberById(@PathVariable String id) {
    return memberService.findById(id);
  }

  @PostMapping
  public ResponseEntity<Void> createMember(@Valid @RequestBody Member member) {
    memberService.register(member);
    return ResponseEntity.ok().build();
  }
}
