import java.util.regex.Pattern

// Spotless supplies LF input. Optional CR also allows testing the rules directly on CRLF files.
def horizontal = '[\\t\\x20]*'
def newline = '\\r?\\n'
def quote = { String text -> Pattern.quote(text) }
def body = { String prefix, String suffix ->
    def line = { String text, boolean last = false ->
        prefix + quote(text) + suffix + (last ? '(?:' + newline + '|\\z)' : newline)
    }
    def blank = prefix + suffix + newline
    def shortNotice = line('SPDX-License-Identifier: Apache-2.0') +
            line('Copyright Red Hat Inc. and Hibernate Authors', true)
    def legacyNotice = '(?:' + line('Hibernate Tools, Tooling for your Hibernate Projects') + blank + ')?' +
            prefix + 'Copyright [0-9]{4}(?:[\\t ]*-[\\t ]*[0-9]{4})? ' +
            quote('Red Hat, Inc.') + suffix + newline + blank +
            line('Licensed under the Apache License, Version 2.0 (the "License");') +
            line('you may not use this file except in compliance with the License.') +
            line('You may obtain a copy of the License at') + blank +
            prefix + 'https?://www\\.apache\\.org/licenses/LICENSE-2\\.0' + suffix + newline + blank +
            line('Unless required by applicable law or agreed to in writing, software') +
            line('distributed under the License is distributed on an "AS IS" basis,') +
            line('WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.') +
            line('See the License for the specific language governing permissions and') +
            line('limitations under the License.', true)
    '(?:' + shortNotice + '|' + legacyNotice + ')'
}
def separator = '(?:' + horizontal + newline + ')*'
def leading = '[\\t\\r\\n ]*'
def block = horizontal + '/\\*' + horizontal + newline +
        body(horizontal + '\\*' + horizontal, horizontal) + horizontal + '\\*/' + separator
// An optional BOM and script prologue are preserved, rather than treated as part of the notice.
def bom = '(?:\\uFEFF|\\u00EF\\u00BB\\u00BF)?'
def prologue = '\\A(?<prefix>' + bom + '(?:#![^\\r\\n]*' + newline + ')?)'
def markup = { String opener ->
    '\\A(?<prefix>' + bom + '(?:<\\?xml[^?]*\\?>' + horizontal + newline + ')?)' + leading +
            quote(opener) + horizontal + newline +
            body(horizontal + '(?:[~*-]' + horizontal + ')?', horizontal) +
            horizontal + '-->' + separator
}
def hashBlank = horizontal + '#' + horizontal + newline
def hashBody = body(horizontal + '#' + horizontal, horizontal + '#?' + horizontal)
def hash = prologue + leading + '(?:' + hashBlank + ')*' +
        '(?:' + horizontal + '#{3,}' + horizontal + newline + ')?' + hashBody +
        '(?:' + horizontal + '#{3,}' + horizontal + '(?:' + newline + '|\\z))?' +
        '(?:' + horizontal + '#' + horizontal + '(?:' + newline + '|\\z))*' + separator

[
    block: prologue + leading + '(?:' + block + ')+',
    xml: markup('<!--'),
    freemarker: markup('<#--'),
    hash: hash,
    // Only a comment immediately inside a grammar header action is eligible.
    antlr: '(?m)^(?<prefix>' + horizontal + '@(?:(?:lexer|parser)::)?header' + horizontal +
            '\\{' + horizontal + newline + ')' + leading + '(?:' + block + ')+',
    // Historical, file-specific exception: preserve the statement preceding the notice.
    utilities: '\\A(?<prefix>' + leading + 'apply plugin: UtilitiesPlugin' + horizontal + newline + separator + ')' +
            leading + '(?:' + block + ')+'
]
