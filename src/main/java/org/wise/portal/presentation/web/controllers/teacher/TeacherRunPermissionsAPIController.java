package org.wise.portal.presentation.web.controllers.teacher;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.wise.portal.dao.ObjectNotFoundException;
import org.wise.portal.domain.run.Run;
import org.wise.portal.domain.user.User;
import org.wise.portal.presentation.web.controllers.ControllerUtil;
import org.wise.portal.presentation.web.exception.TeacherAlreadySharedWithRunException;
import org.wise.portal.presentation.web.response.SharedOwner;
import org.wise.portal.presentation.web.response.SimpleResponse;
import org.wise.portal.service.run.RunService;
import org.wise.portal.service.user.UserService;

/**
 * REST API endpoints for Teacher Permissions
 *
 * @author Hiroki Terashima
 * @author Geoffrey Kwan
 */
@RestController
@RequestMapping("/api/teacher/run/permission")
public class TeacherRunPermissionsAPIController {

  @Autowired
  private RunService runService;

  @Autowired
  private UserService userService;

  @RequestMapping(value = "/{runId}/{teacherUsername}", method = RequestMethod.PUT)
  protected SharedOwner addSharedOwner(HttpServletRequest request, Authentication auth,
      @PathVariable Long runId, @PathVariable String teacherUsername) {
    try {
      SharedOwner sharedOwner = runService.addSharedTeacher(runId, teacherUsername);
      Run run = runService.retrieveById(runId);
      String connectCode = run.getConnectCode();
      if (connectCode != null && !connectCode.equals("")) {
        try {
          String url = "/api/projects/score/addMember";
          JSONObject params = new JSONObject();
          params.put("username", teacherUsername);
          params.put("role", "teacher");
          params.put("code", connectCode);
          ControllerUtil.doCkBoardPost(request, auth, params.toString(), url);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return sharedOwner;
    } catch (ObjectNotFoundException e) {
      return null;
    } catch (TeacherAlreadySharedWithRunException e) {
      return null;
    }
  }

  @PutMapping("/transfer/{runId}/{teacherUsername}")
  protected String transferRunOwnership(HttpServletRequest request, Authentication auth,
      @PathVariable Long runId, @PathVariable String teacherUsername) {
    try {
      JSONObject result = runService.transferRunOwnership(runId, teacherUsername);
      if (result != null) {
        try {
          String connectCode = result.getString("code");
          if (connectCode != null && !connectCode.equals("")) {
            String url = "/api/projects/score/addMember";
            JSONObject params = new JSONObject();
            params.put("username", teacherUsername);
            params.put("role", "teacher");
            params.put("code", connectCode);
            ControllerUtil.doCkBoardPost(request, auth, params.toString(), url);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return result.toString();
    } catch (ObjectNotFoundException e) {
      return null;
    }
  }

  @RequestMapping(value = "/{runId}/{username}", method = RequestMethod.DELETE)
  protected SimpleResponse removeSharedOwner(HttpServletRequest request, Authentication auth,
      @PathVariable Long runId, @PathVariable String username) {
    try {
      runService.removeSharedTeacher(username, runId);
      Run run = runService.retrieveById(runId);
      String connectCode = run.getConnectCode();
      if (connectCode != null && !connectCode.equals("")) {
        try {
          String url = "/api/projects/score/removeMember";
          JSONObject params = new JSONObject();
          params.put("username", username);
          params.put("code", connectCode);
          ControllerUtil.doCkBoardPost(request, auth, params.toString(), url);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
      return new SimpleResponse("success", "successfully removed shared owner");
    } catch (ObjectNotFoundException e) {
      return new SimpleResponse("error", "user or run was not found");
    }
  }

  @RequestMapping(value = "/{runId}/{userId}/{permissionId}", method = RequestMethod.PUT)
  protected SimpleResponse addPermission(@PathVariable Long runId, @PathVariable Long userId,
      @PathVariable Integer permissionId) {
    try {
      runService.addSharedTeacherPermission(runId, userId, permissionId);
      return new SimpleResponse("success", "successfully added run permission");
    } catch (ObjectNotFoundException e) {
      return new SimpleResponse("error", "user or run was not found");
    }
  }

  @RequestMapping(value = "/{runId}/{userId}/{permissionId}", method = RequestMethod.DELETE)
  protected SimpleResponse deletePermission(@PathVariable Long runId, @PathVariable Long userId,
      @PathVariable Integer permissionId) {
    try {
      runService.removeSharedTeacherPermission(runId, userId, permissionId);
      return new SimpleResponse("success", "successfully removed run permission");
    } catch (ObjectNotFoundException e) {
      return new SimpleResponse("error", "user or run was not found");
    }
  }
}
