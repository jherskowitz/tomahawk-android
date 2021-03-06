package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetChartItem;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ChartItemsDeserializer extends JsonDeserializer<Map<String, HatchetChartItem>> {

    @Override
    public Map<String, HatchetChartItem> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        List<HatchetChartItem> list = jp.readValueAs(new TypeReference<List<HatchetChartItem>>() {
        });
        return DeserializerUtils.listToMap(list);
    }
}
