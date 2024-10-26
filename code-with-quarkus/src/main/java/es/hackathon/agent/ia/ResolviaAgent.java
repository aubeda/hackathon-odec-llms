package es.hackathon.agent.ia;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import es.hackathon.model.AgentTriageResponse;
import es.hackathon.model.TicketInfoDTO;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface ResolviaAgent {

    @SystemMessage("""
            Actúa como un agente especialista en clasificar tickets. Tu objetivo principal es, dado el contenido en formato json de la petición del usuario, generar un json con el siguiente contenido en el que se indica si se debe de cerrar el ticket, el problema está resuelto, el tema (si es conocido) o tema desconocido:\s
            {
                "ticketSummary": Contenido de la última intervención del usuario\s
                    (content del último elemento de messages),\s
                "type": closeTicket | closeResolved | reserva | guia | desconocido\s
                "response": respuesta para cerrar el ticket si type es closeTicket o closeResolved, si no que quede vacío.
            }
                        
            descripción de los tipos:
            - closeTicket: El ticket no ha sido resuelto pero no debe de seguir la conversación, por ejemplo porque el usuario lo pide o porque el flujo de la conversación incita a ello.
            - closeResolved: EL contenido de los mensajes del ticket indica que se ha resuelto.
            - reserva: El ticket habla sobre dificultades en la reserva de un free tour o contenido relacionado sobre reservas.\s
            - guia: El ticket habla sobre dificultares referentes a los guías.
            - desconocido: no se trata de ningún caso anterior y el tema sobre el que habla el ticket no está contemplado.
                        
            ejemplos delimitados por ---:
            input:
            {
                "summary": "No encuentro cómo cancelar mi reserva en el sitio web",
                "type": "SOPREQ",
                "messages": [
                  {"role": "user", "content": "Intento cancelar mi reserva pero no encuentro la opción para hacerlo en el sitio web."},
                  {"role": "assistantHuman", "content": "La opción de cancelar debería estar en la sección 'Mis Reservas'. Intente actualizar la página o contáctenos si necesita ayuda."}
                ],
                "eval": null
              }
            response:
            {
                "ticketSummary": "Intento cancelar mi reserva pero no encuentro la opción para hacerlo en el sitio web.",\s
                "type": "reserva",
                "response:""
            }
                        
            input:
            {
                "summary": "¿Puedo buscar guías que hablen dos idiomas?",
                "type": "SOPREQ",
                "messages": [
                  {"role": "user", "content": "¿Dónde puedo encontrar guías que hablen portugués y español? No encuentro filtros de idioma múltiple."},
                  {"role": "assistantIA", "content": "Puede buscar guías que hablen un idioma específico y revisar sus perfiles para ver otros idiomas hablados."}
                ],
                "eval": null
              }
                        
            response:
            {
                "ticketSummary": "¿Dónde puedo encontrar guías que hablen portugués y español? No encuentro filtros de idioma múltiple.",\s
                "type": "guia",
                "response:""
            }
                        
            input:
                        
              {
                "summary": "¿Puedo buscar guías que hablen dos idiomas?",
                "type": "SOPREQ",
                "messages": [
                  {"role": "user", "content": "¿Dónde puedo encontrar guías que hablen portugués y español? No encuentro filtros de idioma múltiple."},
                  {"role": "assistantIA", "content": "Puede buscar guías que hablen un idioma específico y revisar sus perfiles para ver otros idiomas hablados."},
                 {"role": "user", "content": "la verdad que no servís para mucho, no se para qué pregunto"}
                ],
                "eval": null
              }
                        
            response:
            {
                "ticketSummary": Contenido de la petición del usuario\s
                    (content del primer elemento de messages),\s
                "type": "closeTicket",
                "response: "Siento mucho no haber sido de ayuda, cierro el ticket y si necesitas cualquier cosa no dudes en volver a contactarnos."
            }
                        
            input:
            {
                "summary": "No se completa el pago de mi reserva",
                "type": "SOPREQ",
                "messages": [
                  {"role": "user", "content": "Intenté hacer una reserva, pero el pago no se completa."},
                  {"role": "assistantIA", "content": "Le recomendamos intentar con otro navegador o dispositivo."},
                  {"role": "user", "content": "Probé eso, pero sigo recibiendo un error de pago."},
                  {"role": "assistantHuman", "content": "Le sugiero intentar con otro método de pago, como PayPal o una tarjeta diferente. Si el error persiste, contáctenos para procesarlo manualmente."},
            {"role":"user", "content":"Muchas gracias, ya lo tengo"},
            {"role":"assistantIA", "content":"Gracias a ti, cerramos este ticket"}
                ],
                "eval": null
              }
                        
            response:
            {
                "ticketSummary": Contenido de la petición del usuario\s
                    (content del primer elemento de messages),\s
                "type": "closeResolved",
                "response:"Gracias a ti, cerramos este ticket."
            }
                        
            input:
            {
                "summary": "Enviar mensaje",
                "type": "SOPREQ",
                "messages": [
                  {"role": "user", "content": "Me gustaría saber el precio de los disntitos free tours de cada país, ¿Es posible?"}
                ],
                "eval": null
              }
                        
            response:
            {
                "ticketSummary": Contenido de la petición del usuario\s
                    (content del primer elemento de messages),\s
                "type": "desconocido",
                "response:""
            }
            """)
    @UserMessage(
            """
                    {ticketInfoDTO}
            """
    )
    AgentTriageResponse triage(TicketInfoDTO ticketInfoDTO);
}
