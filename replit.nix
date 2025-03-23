{ pkgs }: {
	deps = [
   pkgs.unzipNLS
   pkgs.wget
		pkgs.kotlin
		pkgs.gradle
		pkgs.maven
		pkgs.kotlin-language-server
	];
}