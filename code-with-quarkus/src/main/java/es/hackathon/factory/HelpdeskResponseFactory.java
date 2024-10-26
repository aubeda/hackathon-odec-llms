package es.hackathon.factory;

import es.hackathon.model.Comment;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class HelpdeskResponseFactory {

    /**
     * Creates a comment with the specified message and internal flag.
     *
     * @param message  The message content of the comment.
     * @param internal Indicates whether the comment is internal or not.
     * @return A newly created Comment object.
     */
    public Comment createComment(String message, boolean internal) {
        return Comment.builder()
                .body(Comment.Body.builder()
                        .version(1)
                        .type("doc")
                        .content(List.of(Comment.Content.builder()
                                .type("paragraph")
                                .content(List.of(Comment.Content.builder()
                                        .type("text")
                                        .text(message)
                                        .build()))
                                .build()))
                        .build())
                .properties(List.of(Comment.Property.builder()
                        .key("sd.public.comment")
                        .value(Comment.Value.builder()
                                .internal(internal)
                                .build())
                        .build()))
                .visibility(null)
                .build();
    }

    /**
     * Creates a comment with the specified message and internal flag.
     *
     * @param texts  The message content of the comment.
     * @param internal Indicates whether the comment is internal or not.
     * @return A newly created Comment object.
     */
    public Comment createCommentMultiText(List<String> texts, boolean internal) {
        return Comment.builder()
                .body(Comment.Body.builder()
                        .version(1)
                        .type("doc")
                        .content(texts.stream().map(text -> Comment.Content.builder()
                                .type("paragraph")
                                .content(List.of(Comment.Content.builder()
                                        .type("text")
                                        .text(text)
                                        .build()))
                                .build())
                                .toList())
                        .build())
                .properties(List.of(Comment.Property.builder()
                        .key("sd.public.comment")
                        .value(Comment.Value.builder()
                                .internal(internal)
                                .build())
                        .build()))
                .visibility(null)
                .build();
    }
}
