package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.pageable.PageDto;
import io.learn.lexigeek.common.pageable.PageableRequest;
import io.learn.lexigeek.common.pageable.PageableUtils;
import io.learn.lexigeek.common.pageable.SortOrder;
import io.learn.lexigeek.common.validation.ErrorCodes;
import io.learn.lexigeek.word.PublicWordFacade;
import io.learn.lexigeek.word.WordFacade;
import io.learn.lexigeek.word.dto.PublicWordFilterForm;
import io.learn.lexigeek.word.dto.WordDto;
import io.learn.lexigeek.word.dto.WordForm;
import io.learn.lexigeek.word.dto.WordPartForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PublicWordService implements PublicWordFacade {

    private final WordRepository wordRepository;
    private final ViewedPublicWordRepository viewedPublicWordRepository;
    private final CategoryFacade categoryFacade;
    private final AccountFacade accountFacade;
    private final WordFacade wordFacade;

    @Override
    @Transactional(readOnly = true)
    public PageDto<WordDto> getPublicWords(final UUID languageUuid,
                                           final UUID categoryUuid,
                                           final PublicWordFilterForm form,
                                           final PageableRequest pageableRequest) {
        pageableRequest.addDefaultSorts(new SortOrder(Word.Fields.created, Sort.Direction.DESC));

        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final AccountDto currentAccount = accountFacade.getLoggedAccount();

        final PublicWordSpecification specification = new PublicWordSpecification(form, languageUuid, currentAccount.id());

        return PageableUtils.toDto(
                wordRepository.findAll(specification, PageableUtils.createPageable(pageableRequest))
                        .map(WordMapper::entityToDto),
                pageableRequest
        );
    }

    @Override
    @Transactional
    public WordDto acceptWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final AccountDto currentAccount = accountFacade.getLoggedAccount();

        final Word publicWord = wordRepository.findByUuid(wordUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        if (!publicWord.getAccepted()) {
            throw new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid);
        }

        final WordForm wordForm = new WordForm(
                publicWord.getComment(),
                publicWord.getMechanism(),
                publicWord.getWordParts().stream()
                        .map(part -> new WordPartForm(
                                part.getAnswer(),
                                part.getBasicWord(),
                                part.getPosition(),
                                part.getToSpeech(),
                                part.getSeparator(),
                                part.getSeparatorType(),
                                part.getWord()
                        ))
                        .collect(Collectors.toList())
        );

        final WordDto createdWord = wordFacade.createWord(languageUuid, categoryUuid, wordForm);

        if (!viewedPublicWordRepository.existsByAccountIdAndWordId(currentAccount.id(), publicWord.getId())) {
            final ViewedPublicWord viewed = new ViewedPublicWord();
            viewed.setAccountId(currentAccount.id());
            viewed.setWord(publicWord);
            viewedPublicWordRepository.save(viewed);
        }

        return createdWord;
    }

    @Override
    public void rejectWord(final UUID languageUuid, final UUID categoryUuid, final UUID wordUuid) {
        categoryFacade.verifyCategoryAccess(languageUuid, categoryUuid);

        final AccountDto currentAccount = accountFacade.getLoggedAccount();

        final Word publicWord = wordRepository.findByUuid(wordUuid)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid));

        if (!publicWord.getAccepted()) {
            throw new NotFoundException(ErrorCodes.WORD_NOT_FOUND, wordUuid);
        }

        if (viewedPublicWordRepository.existsByAccountIdAndWordId(currentAccount.id(), publicWord.getId())) {
            return;
        }

        final ViewedPublicWord viewed = new ViewedPublicWord();
        viewed.setAccountId(currentAccount.id());
        viewed.setWord(publicWord);

        viewedPublicWordRepository.save(viewed);
    }
}
