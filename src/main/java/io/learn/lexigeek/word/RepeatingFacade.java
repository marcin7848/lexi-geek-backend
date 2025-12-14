package io.learn.lexigeek.word;

import io.learn.lexigeek.word.dto.*;

import java.util.UUID;

public interface RepeatingFacade {

    RepeatSessionDto startSession(final UUID languageUuid, final StartRepeatSessionForm form);

    RepeatSessionDto getActiveSession(final UUID languageUuid);

    RepeatWordDto getNextWord(final UUID languageUuid);

    CheckAnswerResultDto checkAnswer(final UUID languageUuid, final UUID wordUuid, final CheckAnswerForm form);

    void resetSession(final UUID languageUuid);
}

