package academy.tochkavhoda.competition.server;

import academy.tochkavhoda.competition.service.ParticipantService;

/** Точка входа для клиента. Бизнес-логика находится в сервисах. */
public class Server {
    private final ParticipantService participantService = new ParticipantService();

    public ServerResponse registerParticipant(String requestJsonString) {
        return participantService.registerParticipant(requestJsonString);
    }
}
