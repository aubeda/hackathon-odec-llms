package es.hackathon.agent.ia;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import es.hackathon.model.*;
import io.quarkiverse.langchain4j.RegisterAiService;

import java.util.List;

@RegisterAiService
public interface ResolviaAgent {

    @SystemMessage("""
            Actúa como un agente especialista en clasificar tickets. Tu objetivo principal es, dado el contenido en formato json de la petición del usuario, generar un json con el siguiente contenido en el que se indica si se debe de cerrar el ticket, el problema está resuelto, el tema (si es conocido) o tema desconocido:\s
            {
                "ticketSummary": Contenido de la última intervención del usuario\s
                    (content del último elemento de messages),\s
                "type": closeTicket | resolvedTicket | reserva | guia | desconocido\s
                "response": respuesta para cerrar el ticket si type es closeTicket o resolvedTicket, si no que quede vacío.
            }
                       
            descripción de los tipos:
            - closeTicket: El ticket no ha sido resuelto pero no debe de seguir la conversación, por ejemplo porque el usuario lo pide o porque el flujo de la conversación incita a ello.
            - resolvedTicket: EL contenido de los mensajes del ticket indica que se ha resuelto.
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
                "type": "resolvedTicket",
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

    @SystemMessage("""
              Actúa como un asistente especialista en sugerir respuestas a tickets. Tu objetivo principal es, dado el contenido de la clave "ticketSummary" del json dado en la petición del usuario, generar un json con el siguiente contenido, añadiendo en content la sugerencia de respuesta a ticketSummary que le darías al agente humano con comentario introductorio:\s
                  
                  {
                      "role": assistantHuman,
                       "nextState": "escalar",
                       "suggestion":true|false,
                       "content":"Sugerencia de respuesta al contenido de ticketSummary con comentario introductorio"
                  }
                  
                  las claves son:
                  - "role": siempre valor assistantHuman
                  - "suggestion": siempre con valor true
                  - "nextState": siempre con valor "escalar"
                  - "content": Sugerencia para humano de respuesta al contenido de ticketSummary con comentario introductorio.
                  
                  Ejemplos delimitados por ---:
                  ---
                  input:
                  {
                      "ticketSummary":"Me gustaría saber el precio de los distintos free tours de cada país, ¿Es posible?",\s
                      "type": "desconocido",
                      "response:""
                  }
                  
                  response:
                  {
                      "role": "assistantHuman",
                       "nextState": "escalar",
                       "suggestion":true,
                       "content":"Aquí tienes una sugerencia sobre una respuesta que puedes utilizar: Muchas gracias por ponerte en contacto con nosotros. Vamos a tratar de resolver tu duda de la mejor forma posible. Para poder saber el precio de los distintos"
                  }
                  ---
            """)
    @UserMessage(
            """
                    {ticketInfoDTO}
            """
    )
    AgentResponse desconocido(TicketInfoDTO ticketInfoDTO);


    @SystemMessage("""
              Actúa como un asistente especialista en sugerir respuestas a tickets. Tu objetivo principal es, dado el contenido de la clave "ticketSummary" del json dado en la petición del usuario, generar un json con el siguiente contenido, añadiendo en content la sugerencia de respuesta a ticketSummary que le darías al agente humano con comentario introductorio:\s
            
               {
                   "role": assistantHuman,
                    "nextState": "escalar",
                    "suggestion":true|false,
                    "content":"Sugerencia de respuesta al contenido de ticketSummary con comentario introductorio"
               }
            
               las claves son:
               - "role": siempre valor assistantHuman
               - "suggestion": siempre con valor true
               - "nextState": siempre con valor "escalar"
               - "content": Sugerencia para humano de respuesta al contenido de ticketSummary con comentario introductorio.
            
               Ejemplos delimitados por ---:
               ---
               input:
               {
                   "ticketSummary":"Me gustaría saber el precio de los distintos free tours de cada país, ¿Es posible?",\s
                   "type": "desconocido",
                   "response:""
               }
           
               response:
               {
                   "role": "assistantHuman",
                    "nextState": "escalar",
                    "suggestion":true,
                    "content":"Aquí tienes una sugerencia sobre una respuesta que puedes utilizar: Muchas gracias por ponerte en contacto con nosotros. Vamos a tratar de resolver tu duda de la mejor forma posible. Para poder saber el precio de los distintos"
               }
               ---
            """)
    @UserMessage(
            """
                    {ticketInfoDTO}
            """
    )
    DiagnosticResponse diagnosticTicket(TicketInfoDTO ticketInfoDTO);

    @SystemMessage("""
            Actúa como un agente especialista en generar respuestas a tickets. Tu objetivo principal es, dada la template del json dado en la petición del usuario, generar un json con el siguiente contenido, añadiendo en content la respuesta a ticketSummary siguiendo la template que es una posible respuesta a la consulta de ticketSummary con comentario introductorio:\s
           
            {
                "role": assistantIA,
                 "nextState": "resolver",
                 "suggestion":false,
                 "content":"Sugerencia de respuesta al contenido de ticketSummary basándose en la solución de template con comentario introductorio."
            }
            
            las claves son:
            - "role": siempre valor assistantIA
            - "suggestion": siempre con valor false
            - "nextState": siempre con valor "resolver"
            - "content": Sugerencia de respuesta al contenido de ticketSummary basándose en la solución de template con comentario introductorio.
            
            Ejemplos delimitados por ---:
            ---
            input1:
            {
                    "ticketSummary": "Quiero realizar un recorrido de fotografía con un guía, pero no veo perfiles que ofrezcan este enfoque.",
                    "metadata": {
                        "template": "Seleccione 'Fotografía' en los filtros. Si aún no aparece ninguna opción, contáctenos para coordinar con un guía especializado en fotografía."
                    }
                }
            response1:
            {
                "role": assistantIA,
                 "nextState": "resolver",
                 "suggestion":false,
                 "content":"Para poder realizar un recorrdio de fotografía con guía debes de seleccionar el filtro 'Fotografía'. Si después de realizar este paso no logras encontrar un guía especializado en fotografía, no dudes en contactarnos."
            }
            
            input2:
            {
                    "ticketSummary": "No consigo encontrar la forma de buscar un guía que ofrezca un tour de fotografía",
                    "metadata": {
                        "template": "Seleccione 'Fotografía' en los filtros. Si aún no aparece ninguna opción, contáctenos para coordinar con un guía especializado en fotografía."
                    }
            }
            response2:
            {
                "role": assistantIA,
                 "nextState": "resolver",
                 "suggestion":false,
                 "content":"Aquí tienes una sugerencia sobre una respuesta que puedes utilizar: Por favor, prueba a seleccionar 'Fotografía' en los filtros. Si después de realizar esto no has logrado encontrar la forma de buscar un guía que ofrezca un tour de fotografía no dudes en contactarnos."
            }
            ---
            """)
    @UserMessage(
            """
                    {top9}
            """
    )
    AgentResponse generateResponseTop9(KnowledgeRequestLLM top9);

    @SystemMessage("""
            Actúa como un asistente especialista en sugerir respuestas a tickets. Tu objetivo principal es, dadas las templates del json proporcionado en la petición del usuario, generar un json con el siguiente contenido, añadiendo en content la sugerencia de respuesta a ticketSummary que es una posible respuesta a la consulta de ticketSummary, inspirándote en las templates o alguna de las templates si crees que pueden ser útiles teniendo en cuenta que no son una solución directa:\s
            
            {
                "role": assistantHuman,
                 "nextState": "escalar",
                 "suggestion":true,
                 "content":"Sugerencia de respuesta al contenido de ticketSummary, inspirándote en las templates o alguna de las templates si crees que pueden ser útiles teniendo en cuenta que no son una solución directa con comentario introductorio"
            }
            
            las claves son:
            - "role": siempre valor assistantHuman
            - "suggestion": siempre con valor true
            - "nextState": siempre con valor "escalar"
            - "content": Sugerencia de respuesta al contenido de ticketSummary, inspirándote en las templates o alguna de las templates si crees que pueden ser útiles teniendo en cuenta que no son una solución directa con comentario introductorio.
            
            Ejemplos delimitados por ---:
            input:
            {
                  {"ticketSummary": "Quiero hacer una reserva de un free tour en Madrid, pero el sistema solo me permite reservar en Valencia.",
                    "template": "Para realizar una reserva debes de acceder al apartado "Reservar free tour" y seleccionar aquella que más te guste."
                    },\s
            {
                "ticketSummary": "Quiero hacer una reserva de un free tour en Madrid, pero el sistema solo me permite reservar en Valencia.",
                "template":"Para reservar un free tour en una localidad concreta, se deben usar los filtros del apartado "Filtros"
                    }
            }
            
            response:
            {
                "role": assistantHuman,
                 "nextState": "escalar",
                 "suggestion":true,
                 "content":"Aquí tienes una sugerencia de una posible respuesta: Para realizar una resera de un free tour en Madrid tal vez puedes revisar los filtros del apartado "Filtros" o revisar en el apartado "Reservar free tour" si encuentras algún free tour en Madrid. Si con esto no resuelves tu duda, vuelve a contactar con nosotros."
            }
            ---
            """)
    @UserMessage(
            """
                    {possibles}
            """
    )
    AgentResponse generateResponsePosibles(List<KnowledgeRequestLLM> possibles);
}
