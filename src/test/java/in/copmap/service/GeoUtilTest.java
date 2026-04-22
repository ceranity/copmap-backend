package in.copmap.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoUtilTest {

    @Test
    void zeroDistanceForSamePoint() {
        assertThat(GeoUtil.haversineMeters(19.0760, 72.8777, 19.0760, 72.8777)).isZero();
    }

    @Test
    void mumbaiToPuneIsRoughly120km() {
        // Mumbai (19.0760, 72.8777) → Pune (18.5204, 73.8567) is ~118 km as the crow flies.
        double meters = GeoUtil.haversineMeters(19.0760, 72.8777, 18.5204, 73.8567);
        assertThat(meters).isBetween(115_000d, 122_000d);
    }

    @Test
    void symmetric() {
        double a = GeoUtil.haversineMeters(28.6139, 77.2090, 12.9716, 77.5946);
        double b = GeoUtil.haversineMeters(12.9716, 77.5946, 28.6139, 77.2090);
        assertThat(a).isEqualTo(b);
    }
}
