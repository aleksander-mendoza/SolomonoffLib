var sol = undefined
var langTools = ace.require("ace/ext/language_tools");
var Range = ace.require("ace/range").Range;
var editor = ace.edit("editor");
editor.session.setMode("ace/mode/mealy");
editor.setTheme("ace/theme/monokai");
editor.setBehavioursEnabled(true)

editorChangeCounter = 0
editor.getSession().on('change', function() {
    editorChangeCounter = (editorChangeCounter + 1) % 5
    if (editorChangeCounter === 4) {
        setCookie('code', editor.getSession().getValue())
    }
});

editor.setOptions({
    enableBasicAutocompletion: [{
        getCompletions: (editor, session, pos, prefix, callback) => {
            // note, won't fire if caret is at a word that does not have these letters
            callback(null, [{
                    name: 'subtract[',
                    value: 'subtract[]',
                    score: 1,
                    meta: 'difference of two languages'
                },
                {
                    name: 'rpni!(',
                    value: 'rpni!()',
                    score: 1,
                    meta: 'RPNI inference algorithm'
                },
                {
                    name: 'rpni_mealy!(',
                    value: 'rpni_mealy!()',
                    score: 1,
                    meta: 'RPNI for Mealy machiens'
                },
                {
                    name: 'ostia!(',
                    value: 'ostia!()',
                    score: 1,
                    meta: 'OSTIA inference for transducers'
                },
                {
                    name: 'compose[',
                    value: 'compose[]',
                    score: 1,
                    meta: 'transducer composition'
                },
                {
                    name: 'inverse[',
                    value: 'inverse[]',
                    score: 1,
                    meta: 'transducer inversion'
                },
                {
                    name: '/**/',
                    value: '/**/',
                    score: 1,
                    meta: 'block comment'
                },
                {
                    name: '//',
                    value: '//',
                    score: 1,
                    meta: 'line comment'
                },
            ]);
        },
    }],
    enableSnippets: true,
    enableLiveAutocompletion: true
});

isSelected = false
editor.selection.on('changeSelection', function() {
    if (editor.getSelectedText() === "") {
        if (isSelected) {
            compile.innerText = "Compile"
            isSelected = false
        }
    } else {
        if (!isSelected) {
            compile.innerText = "Compile Selection"
            isSelected = true
        }
    }
});



var inputField = ace.edit("inputField");
inputField.setOptions({
    maxLines: 1, // make it 1 line
    autoScrollEditorIntoView: true,
    highlightActiveLine: false,
    printMargin: false,
    showGutter: false,
    mode: "ace/mode/mealy",
    theme: "ace/theme/monokai"
});

inputField.setBehavioursEnabled(true)

inputField.on("paste", function(e) {
    e.text = e.text.replace(/[\r\n]+/g, " ");
});
inputField.commands.bindKey("Enter|Shift-Enter", function(e) {
    repl(inputField.getValue())
    inputField.setValue('')
});
inputField.commands.bindKey("Down", function(e) {
    if (replHistoryIndex + 1 < replHistory.length) {
        replHistoryIndex++
        inputField.setValue(replHistory[replHistoryIndex])
        inputField.selection.clearSelection()
    } else if (replHistoryIndex + 1 === replHistory.length) {
        replHistoryIndex++
        inputField.setValue(replCurrent)
        inputField.selection.clearSelection()
    }
});
inputField.commands.bindKey("Up", function(e) {
    if (replHistoryIndex > 0) {
        if (replHistoryIndex === replHistory.length) {
            replCurrent = inputField.value
        }
        replHistoryIndex--
        inputField.setValue(replHistory[replHistoryIndex])
        inputField.selection.clearSelection()
    }
});

function update(event) {
    var value = inputField.session.getValue()
    var shouldShow = !value.length;
    var node = inputField.renderer.emptyMessageNode;
    if (!shouldShow && node) {
        inputField.renderer.scroller.removeChild(inputField.renderer.emptyMessageNode);
        inputField.renderer.emptyMessageNode = null;
    } else if (shouldShow && !node) {
        node = inputField.renderer.emptyMessageNode = document.createElement("div");
        node.textContent = "Console input (enter to submit):"
        node.className = "ace_emptyMessage"
        node.style.padding = "0 9px"
        node.style.position = "absolute"
        node.style.zIndex = 9
        node.style.opacity = 0.5
        inputField.renderer.scroller.appendChild(node);
    }
    if(value.startsWith('::')){
       inputField.session.setValue(value.substr(1))
    }
}

inputField.getSession().on('change', update);
setTimeout(update, 100);
inputField.setOptions({
    enableBasicAutocompletion: [{
        getCompletions: (editor, session, pos, prefix, callback) => {
            // note, won't fire if caret is at a word that does not have these letters
            callback(null, [{
                    name: 'subtract[',
                    value: 'subtract[]',
                    score: 1,
                    meta: 'difference of two languages'
                },
                {
                    name: 'rpni!(',
                    value: 'rpni!()',
                    score: 1,
                    meta: 'RPNI inference algorithm'
                },
                {
                    name: 'rpni_mealy!(',
                    value: 'rpni_mealy!()',
                    score: 1,
                    meta: 'RPNI for Mealy machiens'
                },
                {
                    name: 'ostia!(',
                    value: 'ostia!()',
                    score: 1,
                    meta: 'OSTIA inference for transducers'
                },
                {
                    name: 'compose[',
                    value: 'compose[]',
                    score: 1,
                    meta: 'transducer composition'
                },
                {
                    name: 'inverse[',
                    value: 'inverse[]',
                    score: 1,
                    meta: 'transducer inversion'
                },
                {
                    name: ':eval',
                    value: ':eval',
                    snippet: ':eval ${1:transducer_name} \'${2:input_string}\'',
                    score: 1,
                    meta: 'evaluate transducer'
                },
                {
                    name: ':ls',
                    value: ':ls',
                    score: 1,
                    meta: 'list defined transducers'
                },
                {
                    name: ':unset',
                    value: ':unset',
                    snippet: ':unset ${1:transducer_name}',
                    score: 1,
                    meta: 'undefine transducer'
                },
                {
                    name: ':reset',
                    value: ':reset',
                    score: 1,
                    meta: 'undefine all transducers'
                },
                {
                    name: ':vis',
                    value: ':vis',
                    snippet: ':vis ${1:transducer_name}',
                    score: 1,
                    meta: 'visualizes automaton'
                },
                {
                    name: ':pipes',
                    value: ':pipes',
                    score: 1,
                    meta: 'list pipelines'
                },
                {
                    name: ':size',
                    value: ':size',
                    snippet: ':size ${1:transducer_name}',
                    score: 1,
                    meta: 'print size of automaton'
                },
                {
                    name: ':load',
                    value: ':load',
                    score: 1,
                    meta: 'reload source file'
                },
                {
                    name: ':is_det',
                    value: ':is_det',
                    snippet: ':is_det ${1:transducer_name}',
                    score: 1,
                    meta: 'check determinism'
                },
                {
                    name: ':equal x y',
                    value: ':equal',
                    snippet: ':equal ${1:transducer_name1} ${2:transducer_name2}',
                    score: 1,
                    meta: 'check equivalence'
                },
                {
                    name: ':run',
                    value: ':run',
                    snippet: ':run @${1:transducer_name} \'${2:input_string}\'',
                    score: 1,
                    meta: 'evaluate pipeline'
                },
                {
                    name: ':clear',
                    value: ':clear',
                    score: 1,
                    meta: 'clears console'
                },
                {
                    name: ':rand_sample of_size',
                    value: ':rand_sample',
                    snippet: ':rand_sample ${1:transducer_name} of_size ${2:number}',
                    score: 1,
                    meta: 'randomly generate sample'
                },
                {
                    name: ':rand_sample of_length',
                    value: ':rand_sample',
                    snippet: ':rand_sample ${1:transducer_name} of_length ${2:number}',
                    score: 1,
                    meta: 'randomly generate sample'
                },
            ]);
        },
    }],
    enableSnippets: true,
    enableLiveAutocompletion: true
});