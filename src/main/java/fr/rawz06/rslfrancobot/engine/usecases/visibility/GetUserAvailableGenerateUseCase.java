package fr.rawz06.rslfrancobot.engine.usecases.visibility;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetUserAvailableGenerateUseCase {

    Map<String, List<String>> availability = Map.of("rot", List.of("rawz06", "manu_4486", "greenpepperch", "blueguy0014", "tonio9193", "aosuna", "zorrodisco"));

    public boolean available(String user, String preset) {
        return availability.get(preset).contains(user);
    }
}
