package org.wise.portal.presentation.web.controllers.teacher.management;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.wise.portal.dao.ObjectNotFoundException;
import org.wise.portal.domain.run.Run;
import org.wise.portal.domain.user.User;
import org.wise.portal.presentation.web.controllers.ControllerUtil;
import org.wise.portal.service.run.RunService;
import org.wise.portal.service.student.StudentService;
import org.wise.portal.service.user.UserService;

@Secured({ "ROLE_TEACHER" })
@RestController
public class RemoveStudentRunController {

  @Autowired
  private RunService runService;

  @Autowired
  private StudentService studentService;

  @Autowired
  private UserService userService;

  @DeleteMapping("/api/teacher/run/{runId}/student/{studentId}/remove")
  void changeWorkgroupPeriod(HttpServletRequest request, Authentication auth,
      @PathVariable Long runId, @PathVariable Long studentId) throws ObjectNotFoundException {
    Run run = runService.retrieveById(runId);
    if (runService.hasWritePermission(auth, run)) {
      User studentUser = userService.retrieveById(studentId);
      studentService.removeStudentFromRun(studentUser, run);
      String connectCode = run.getConnectCode();
      if (connectCode != null && !connectCode.equals("")) {
        try {
          String url = "/api/projects/score/removeMember";
          JSONObject params = new JSONObject();
          params.put("username", studentUser.getUserDetails().getUsername());
          params.put("code", connectCode);
          ControllerUtil.doCkBoardPost(request, auth, params.toString(), url);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } else {
      throw new AccessDeniedException("User does not have permission to remove student from run");
    }
  }
}
