package samuel.utils;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class Plotter {

    @GetMapping("/plot")
    public Map<Double, Double> plot(
            @RequestParam(name = "equation") String equation,
            @RequestParam(name = "from", defaultValue = "0") String from,
            @RequestParam(name = "to", defaultValue = "100") String to,
            @RequestParam(name = "step", defaultValue = "1") String step) {
        var eq = new Graph(equation, from, to, step);
        return eq.evaluate();
    }


}
