package at.jku.se.smarthome.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards Angular deep-link routes to {@code index.html} so the Angular router
 * can handle client-side navigation.
 *
 * <p>Without this controller, reloading or directly accessing an Angular route
 * (e.g. {@code /devices}, {@code /rooms/1}) would return a 404 from Spring Boot
 * because those paths have no server-side handler. This controller intercepts
 * all path segments that contain no dot (i.e. are not static file requests) and
 * forwards them to the Angular entry point.</p>
 */
@Controller
public class SpaController {

    /**
     * Catches single- and multi-segment URL paths that do not contain a dot
     * (no file extension) and are not handled by a more specific mapping
     * ({@code /api/**}, {@code /ws/**}, {@code /error}).
     * Forwards to {@code /index.html} so the Angular router can take over.
     *
     * <p>The negative-lookahead in the path regex prevents this mapping from
     * shadowing the WebSocket upgrade handler at {@code /ws/**} and all REST
     * controllers under {@code /api/**}.</p>
     *
     * @return a forward directive to the Angular entry point
     */
    @GetMapping(value = {
        "/{path:(?!api|ws|error|actuator)[^\\.]*}",
        "/{path:(?!api|ws|error|actuator)[^\\.]*}/**"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
