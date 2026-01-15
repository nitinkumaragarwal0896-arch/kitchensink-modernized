package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;

import java.util.List;

public interface IMemberService {

  List<Member> findAll();

  Member findById(String id);

  Member register(Member member);
}
