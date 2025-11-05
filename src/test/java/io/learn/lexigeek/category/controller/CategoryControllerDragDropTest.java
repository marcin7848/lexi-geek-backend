package io.learn.lexigeek.category.controller;

import io.learn.lexigeek.category.CategoryFacade;
import io.learn.lexigeek.category.dto.UpdateCategoryPositionForm;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.exception.ValidationException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Example test class for CategoryController drag-and-drop functionality
 * NOTE: This is an example/template. You may need to adjust based on your security configuration.
 *
 * IMPORTANT: Position uniqueness is guaranteed by the implementation through:
 * 1. Always excluding the moving category from position updates
 * 2. Using range-based updates for same-parent moves
 * 3. Atomic transactions ensuring consistency
 *
 * For integration tests, verify that after each move:
 * - No two categories in the same parent have the same position
 * - Positions are sequential with no gaps (0, 1, 2, 3, ...)
 */

class CategoryControllerDragDropTest {

    //@Autowired
    private MockMvc mockMvc;

    @Mock
    private CategoryFacade categoryFacade;

    @Test
    void shouldUpdateCategoryPosition_WhenValidRequest() throws Exception {
        // Given
        UUID languageUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();
        UUID parentUuid = UUID.randomUUID();

        String requestBody = String.format("""
                {
                    "parentUuid": "%s",
                    "position": 0
                }
                """, parentUuid);

        doNothing().when(categoryFacade).updateCategoryPosition(
                eq(languageUuid),
                eq(categoryUuid),
                any(UpdateCategoryPositionForm.class)
        );

        // When & Then
        mockMvc.perform(patch("/languages/{languageUuid}/categories/{uuid}/position", languageUuid, categoryUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldUpdateCategoryPosition_ToRootLevel_WhenParentUuidIsNull() throws Exception {
        // Given
        UUID languageUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();

        String requestBody = """
                {
                    "parentUuid": null,
                    "position": 2
                }
                """;

        doNothing().when(categoryFacade).updateCategoryPosition(
                eq(languageUuid),
                eq(categoryUuid),
                any(UpdateCategoryPositionForm.class)
        );

        // When & Then
        mockMvc.perform(patch("/languages/{languageUuid}/categories/{uuid}/position", languageUuid, categoryUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFound_WhenCategoryDoesNotExist() throws Exception {
        // Given
        UUID languageUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();

        String requestBody = """
                {
                    "parentUuid": null,
                    "position": 0
                }
                """;

        doThrow(new NotFoundException(ErrorCodes.CATEGORY_NOT_FOUND, categoryUuid))
                .when(categoryFacade)
                .updateCategoryPosition(eq(languageUuid), eq(categoryUuid), any(UpdateCategoryPositionForm.class));

        // When & Then
        mockMvc.perform(patch("/languages/{languageUuid}/categories/{uuid}/position", languageUuid, categoryUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequest_WhenCircularReferenceDetected() throws Exception {
        // Given
        UUID languageUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();
        UUID parentUuid = UUID.randomUUID();

        String requestBody = String.format("""
                {
                    "parentUuid": "%s",
                    "position": 0
                }
                """, parentUuid);

        doThrow(new ValidationException(ErrorCodes.CIRCULAR_REFERENCE_ERROR, "Cannot move category to its own descendant"))
                .when(categoryFacade)
                .updateCategoryPosition(eq(languageUuid), eq(categoryUuid), any(UpdateCategoryPositionForm.class));

        // When & Then
        mockMvc.perform(patch("/languages/{languageUuid}/categories/{uuid}/position", languageUuid, categoryUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_WhenPositionIsNegative() throws Exception {
        // Given
        UUID languageUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();

        String requestBody = """
                {
                    "parentUuid": null,
                    "position": -1
                }
                """;

        // When & Then
        mockMvc.perform(patch("/languages/{languageUuid}/categories/{uuid}/position", languageUuid, categoryUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_WhenPositionIsMissing() throws Exception {
        // Given
        UUID languageUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();

        String requestBody = """
                {
                    "parentUuid": null
                }
                """;

        // When & Then
        mockMvc.perform(patch("/languages/{languageUuid}/categories/{uuid}/position", languageUuid, categoryUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFound_WhenParentCategoryDoesNotExist() throws Exception {
        // Given
        UUID languageUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();
        UUID parentUuid = UUID.randomUUID();

        String requestBody = String.format("""
                {
                    "parentUuid": "%s",
                    "position": 0
                }
                """, parentUuid);

        doThrow(new NotFoundException(ErrorCodes.PARENT_CATEGORY_NOT_FOUND, parentUuid))
                .when(categoryFacade)
                .updateCategoryPosition(eq(languageUuid), eq(categoryUuid), any(UpdateCategoryPositionForm.class));

        // When & Then
        mockMvc.perform(patch("/languages/{languageUuid}/categories/{uuid}/position", languageUuid, categoryUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }
}

