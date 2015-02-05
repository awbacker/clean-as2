package org.cleanas2.common.annotation;

import java.lang.annotation.*;

/**
 * Annotation interface that is applied to all commands, which provides the name
 * of the command (used to run it) and a description (shown in the 'help') list,
 * as well as the "group" the command belongs to.
 *
 * e.g.  "#> cert view my-partner" =>  @Command(name="view", group="cert", description="show an individual cert")
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface Command {
    String name();
    String group() default "";
    String description() default "No description provided";
}
