package org.wise.portal.service.session.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.wise.portal.domain.authentication.impl.StudentUserDetails;
import org.wise.portal.domain.authentication.impl.TeacherUserDetails;
import org.wise.portal.presentation.web.controllers.ControllerUtil;
import org.wise.portal.service.session.SessionService;

@Service
public class SessionServiceImpl<S extends Session> implements SessionService {

  @Autowired
  private Environment appProperties;

  @Autowired
  private StringRedisTemplate stringRedisTemplate;

  public void addSignedInUser(UserDetails userDetails) {
    stringRedisTemplate.opsForSet().add("signedInUsers", userDetails.getUsername());
    if (userDetails instanceof StudentUserDetails) {
      stringRedisTemplate.opsForSet().add("signedInStudents", userDetails.getUsername());
    } else if (userDetails instanceof TeacherUserDetails) {
      stringRedisTemplate.opsForSet().add("signedInTeachers", userDetails.getUsername());
    }
  }

  @Override
  public Set<String> getLoggedInStudents() {
    return stringRedisTemplate.opsForSet().members("signedInStudents");
  }

  @Override
  public Set<String> getLoggedInTeachers() {
    return stringRedisTemplate.opsForSet().members("signedInTeachers");
  }

  public void removeSignedInUser(UserDetails userDetails) {
    String username = userDetails.getUsername();
    stringRedisTemplate.opsForSet().remove("signedInUsers", username);
    if (userDetails instanceof StudentUserDetails) {
      stringRedisTemplate.opsForSet().remove("signedInStudents", username);
    } else if (userDetails instanceof TeacherUserDetails) {
      stringRedisTemplate.opsForSet().remove("signedInTeachers", username);
    }
  }

  @Override
  public Set<String> getCurrentlyAuthoredProjects() {
    return stringRedisTemplate.opsForSet().members("currentlyAuthoredProjects");
  }

  public int getNumberSignedInUsers() {
    return stringRedisTemplate.opsForSet().members("signedInUsers").size();
  }

  public void addCurrentAuthor(Serializable projectId, String authorUsername) {
    stringRedisTemplate.opsForSet().add("currentlyAuthoredProjects", projectId.toString());
    stringRedisTemplate.opsForSet().add("currentAuthors:" + projectId, authorUsername);
  }

  @Override
  public void removeCurrentAuthor(UserDetails author) {
    Set<String> currentlyAuthoredProjects = stringRedisTemplate.opsForSet()
        .members("currentlyAuthoredProjects");
    for (String projectId : currentlyAuthoredProjects) {
      removeCurrentAuthor(projectId, author.getUsername());
    }
  }

  public void removeCurrentAuthor(Serializable projectId, String authorUsername) {
    stringRedisTemplate.opsForSet().remove("currentAuthors:" + projectId, authorUsername);
    Long numCurrentAuthorsForProject = stringRedisTemplate.opsForSet()
        .size("currentAuthors:" + projectId);
    if (numCurrentAuthorsForProject == 0) {
      stringRedisTemplate.opsForSet().remove("currentlyAuthoredProjects", projectId.toString());
    }
  }

  @Override
  public void removeUser(UserDetails user) {
    removeSignedInUser(user);
    if (user instanceof TeacherUserDetails) {
      removeCurrentAuthor(user);
    }
  }

  public Set<String> getCurrentAuthors(Serializable projectId) {
    return stringRedisTemplate.opsForSet().members("currentAuthors:" + projectId);
  }

  public boolean isCkBoardAvailable() {
    String ckBoardUrl = appProperties.getProperty("ck_board_url");
    return ckBoardUrl != null && !ckBoardUrl.equals("");
  }

  public void signOutOfCkBoard(HttpServletRequest request) {
    String ckSessionCookie = ControllerUtil.getCkSessionCookie(request);
    HttpClient client = HttpClientBuilder.create().build();
    HttpPost ckBoardLogoutRequest = new HttpPost(ControllerUtil.getCkBoardUrl("/api/auth/logout"));
    ckBoardLogoutRequest.setHeader("Authorization", "Bearer " + ckSessionCookie);
    try {
      client.execute(ckBoardLogoutRequest);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
