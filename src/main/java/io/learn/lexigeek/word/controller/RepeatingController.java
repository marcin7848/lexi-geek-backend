package io.learn.lexigeek.word.controller;

import io.learn.lexigeek.word.RepeatingFacade;
import io.learn.lexigeek.word.dto.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class RepeatingController {

    private static final class Routes {
        private static final String REPEAT_SESSION = "/languages/{languageUuid}/repeat-session";
        private static final String REPEAT_SESSION_NEXT_WORD = REPEAT_SESSION + "/next-word";
        private static final String REPEAT_SESSION_CHECK_ANSWER = REPEAT_SESSION + "/words/{wordUuid}/check-answer";
    }

    private final RepeatingFacade repeatingFacade;

    @PostMapping(Routes.REPEAT_SESSION)
    @ResponseStatus(HttpStatus.CREATED)
    RepeatSessionDto startSession(@PathVariable final UUID languageUuid,
                                  @RequestBody @Valid final StartRepeatSessionForm form) {
        return repeatingFacade.startSession(languageUuid, form);
    }

    @GetMapping(Routes.REPEAT_SESSION)
    RepeatSessionDto getActiveSession(@PathVariable final UUID languageUuid) {
        return repeatingFacade.getActiveSession(languageUuid);
    }

    @GetMapping(Routes.REPEAT_SESSION_NEXT_WORD)
    RepeatWordDto getNextWord(@PathVariable final UUID languageUuid) {
        return repeatingFacade.getNextWord(languageUuid);
    }

    @PostMapping(Routes.REPEAT_SESSION_CHECK_ANSWER)
    CheckAnswerResultDto checkAnswer(@PathVariable final UUID languageUuid,
                                     @PathVariable final UUID wordUuid,
                                     @RequestBody @Valid final CheckAnswerForm form) {
        return repeatingFacade.checkAnswer(languageUuid, wordUuid, form);
    }

    @DeleteMapping(Routes.REPEAT_SESSION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetSession(@PathVariable final UUID languageUuid) {
        repeatingFacade.resetSession(languageUuid);
    }
}

