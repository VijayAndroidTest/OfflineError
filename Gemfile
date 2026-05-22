source "https://rubygems.org"

gem "fastlane"

# Automatically pulls in the plugins listed in fastlane/Pluginfile
plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)