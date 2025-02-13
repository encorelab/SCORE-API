package org.wise.vle.web.wise5;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMockRunner;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.acls.domain.BasePermission;
import org.wise.portal.dao.ObjectNotFoundException;
import org.wise.portal.domain.workgroup.Workgroup;
import org.wise.portal.presentation.web.controllers.APIControllerTest;
import org.wise.vle.domain.notification.Notification;

@RunWith(EasyMockRunner.class)
public class NotificationControllerTest extends APIControllerTest {

  @TestSubject
  private NotificationController controller = new NotificationController();

  @Test
  public void getNotifications_NotTeacherOfRun_ReturnEmptyNotifications()
      throws ObjectNotFoundException {
    expect(userService.retrieveUserByUsername(TEACHER_USERNAME)).andReturn(teacher1);
    expect(runService.hasRunPermission(run1, teacher1, BasePermission.READ)).andReturn(false);
    List<Notification> notifications = new ArrayList<Notification>();
    replay(userService, runService, vleService);
    controller.getNotifications(teacherAuth, run1, null, null, null, null, null, null);
    assertEquals(0, notifications.size());
    verify(userService, runService, vleService);
  }

  @Test
  public void getNotifications_TeacherOfRun_ReturnNotificationsForTeacher()
      throws ObjectNotFoundException {
    expect(userService.retrieveUserByUsername(TEACHER_USERNAME)).andReturn(teacher1);
    expect(runService.hasRunPermission(run1, teacher1, BasePermission.READ)).andReturn(true);
    List<Notification> notifications = new ArrayList<Notification>();
    notifications.add(new Notification());
    expect(vleService.getNotifications(null, run1, null, null, null, null, null))
        .andReturn(notifications);
    replay(userService, runService, vleService);
    controller.getNotifications(teacherAuth, run1, null, null, null, null, null, null);
    assertEquals(1, notifications.size());
    verify(userService, runService, vleService);
  }

  @Test
  public void notifyClassmatesInPeriod_UserAssociatedWithRun_NotifyClassmates() throws Exception {
    expect(userService.retrieveUserByUsername(STUDENT_USERNAME)).andReturn(student1);
    expect(runService.retrieveById(runId1)).andReturn(run1);
    expect(runService.getWorkgroups(runId1, run1Period1.getId()))
        .andReturn(new ArrayList<Workgroup>());
    replay(runService, userService);
    controller.notifyClassmatesInPeriod(runId1, run1Period1.getId(), new Notification(),
        studentAuth);
    verify(runService, userService);
  }
}
