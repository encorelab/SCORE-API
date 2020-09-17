package org.wise.portal.presentation.web.controllers.score;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for single-page SCORE app built with Angular
 *
 * @author Anthony Perritano
 * @author Hiroki Terashima
 */
@Controller
@RequestMapping(value = { "/score-app" })
public class ScoreAPIController {

  /**
   * Invokes the SCORE Teaching Assistant Tool based without a run
   */
  @GetMapping
  protected String showTeachingAssistant(HttpServletRequest request, HttpServletResponse response) {
    return "forward:/score/teachingassistant/dist/index.html";
  }
}
