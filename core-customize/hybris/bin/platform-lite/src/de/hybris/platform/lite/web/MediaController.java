package de.hybris.platform.lite.web;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** /medias/... の画像。本物の画像は置かないので、名前を書いた SVG をその場で作って返す。 */
@RestController
public class MediaController
{
	@GetMapping("/medias/{name:.+}")
	public ResponseEntity<String> media(@PathVariable("name") final String name)
	{
		final int hue = Math.abs(name.hashCode()) % 360;
		final String label = name.replaceAll("\\.svg$", "").replaceAll("[<>&\"']", "");
		final String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"300\" viewBox=\"0 0 300 300\">"
				+ "<rect width=\"300\" height=\"300\" fill=\"hsl(" + hue + ",45%,85%)\"/>"
				+ "<text x=\"150\" y=\"155\" font-family=\"sans-serif\" font-size=\"16\" text-anchor=\"middle\" fill=\"#333\">" + label + "</text></svg>";
		return ResponseEntity.ok().contentType(MediaType.valueOf("image/svg+xml")).cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)))
				.body(svg);
	}
}
