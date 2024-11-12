package com.ocean.scdemo.hierarchy.application;

import com.ocean.scdemo.hierarchy.domain.Circle;
import com.ocean.scdemo.hierarchy.domain.Rectangle;
import com.ocean.scdemo.hierarchy.domain.Shape;
import com.ocean.scdemo.hierarchy.domain.Triangle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShapeService {



    public List<Shape> getShapes() {
        return List.of(
            new Circle(1.0),
            new Rectangle(1.0, 2.0),
            new Triangle(1.0, 2.0)
        );
    }
}
