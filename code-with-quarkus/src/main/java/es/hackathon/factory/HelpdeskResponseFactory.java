package es.hackathon.factory;

import es.hackathon.model.Comment;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
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
        List<Comment.Content> contentList = new ArrayList<>();
        contentList.add(Comment.Content.builder()
                .type("text")
                .text(message)
                .build());
        if (!internal) {
            contentList.add(Comment.Content.builder()
                    .type("Si desea que contactemos por teléfono con usted para una atención más dirigida, proporcione su número de teléfono en respuesta a este email")
                    .text(message)
                    .build());
        }
        return Comment.builder()
                .body(Comment.Body.builder()
                        .version(1)
                        .type("doc")
                        .content(contentList)
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

    private Comment.Content buildMessageContactPhone(){
        return Comment.Content.builder()
                .type("paragraph")
                .content(List.of(Comment.Content.builder()
                        .type("text")
                        .text("Si desea que contactemos por teléfono con usted para una atención más dirigida, proporcione su número de teléfono en respuesta a este email")
                                .marks(List.of(Comment.Mark.builder()
                                        .attrs(Comment.Attrs.builder()
                                                .color("blue")
                                                .build())
                                        .type("textColor")
                                        .build()))
                        .build()))
                .build();
    }
}
